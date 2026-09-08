package com.careychi.hrstrap;

import android.app.Activity;
import android.app.Application;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import com.careychi.hrstrap.ui.Ui;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Centralized startup display policy.
 *
 * Android does not expose LCD/OLED panel technology through a normal public app API, so this
 * performs conservative best-effort detection from readable panel descriptors and OEM display
 * properties. Only explicit LCD evidence enables the pure-black palette. OLED or unknown panels
 * keep the existing palette to avoid false positives.
 *
 * Future changes/removal should be made here; HeartRateApplication has only one startup call.
 */
public final class DisplayBackgroundPolicy {
    private static final String TAG = "DisplayBgPolicy";

    enum PanelType { LCD, OLED, UNKNOWN }

    private static final String[] PANEL_INFO_FILES = {
            "/sys/class/graphics/fb0/msm_fb_panel_info",
            "/sys/class/graphics/fb0/panel_info",
            "/sys/class/graphics/fb0/panel_name",
            "/sys/class/graphics/fb0/lcd_model",
            "/sys/class/drm/card0-DSI-1/panel_name",
            "/sys/class/drm/card0-DSI-1/name",
            "/sys/class/drm/card0-eDP-1/panel_name"
    };

    private static final Set<String> PANEL_PROPERTY_KEYS = new HashSet<>();
    static {
        PANEL_PROPERTY_KEYS.add("ro.boot.display_panel");
        PANEL_PROPERTY_KEYS.add("ro.boot.display.panel");
        PANEL_PROPERTY_KEYS.add("ro.boot.panel");
        PANEL_PROPERTY_KEYS.add("ro.vendor.display.panel");
        PANEL_PROPERTY_KEYS.add("vendor.display.panel");
        PANEL_PROPERTY_KEYS.add("ro.vendor.display.panel_type");
        PANEL_PROPERTY_KEYS.add("vendor.display.panel_type");
        PANEL_PROPERTY_KEYS.add("ro.product.display_panel");
        PANEL_PROPERTY_KEYS.add("ro.display.panel_type");
        PANEL_PROPERTY_KEYS.add("persist.vendor.display.panel_type");
    }

    private static boolean installed;

    private DisplayBackgroundPolicy() {}

    /** Single startup entry point for LCD detection and background-theme application. */
    public static synchronized void applyStartupDisplayPolicy(Application application) {
        PanelType panelType = detectPanelType();
        boolean lcd = panelType == PanelType.LCD;
        Ui.applyLcdPureBlackBackground(lcd);
        Log.i(TAG, "Panel classification=" + panelType + ", lcdBlackBackground=" + lcd);

        if (!installed) {
            application.registerActivityLifecycleCallbacks(new WindowBackgroundCallbacks());
            installed = true;
        }
    }

    static PanelType classifyPanelDescriptor(String descriptor) {
        if (descriptor == null || descriptor.trim().isEmpty()) return PanelType.UNKNOWN;
        String value = descriptor.toLowerCase(Locale.ROOT)
                .replace('_', ' ')
                .replace('-', ' ');

        // Check emissive technologies first because some OLED descriptors also mention a TFT
        // backplane. That must not be mistaken for a TFT-LCD panel.
        if (containsAny(value, "amoled", "oled", "p oled", "poled", "ltpo")) {
            return PanelType.OLED;
        }
        if (containsAny(value, "lcd", "ips", "tft", "mini led", "miniled")) {
            return PanelType.LCD;
        }
        return PanelType.UNKNOWN;
    }

    private static PanelType detectPanelType() {
        StringBuilder evidence = new StringBuilder();

        for (String path : PANEL_INFO_FILES) {
            String text = readSmallTextFile(path);
            if (!text.isEmpty()) evidence.append('\n').append(text);
        }

        String properties = readPanelProperties();
        if (!properties.isEmpty()) evidence.append('\n').append(properties);
        return classifyPanelDescriptor(evidence.toString());
    }

    private static String readSmallTextFile(String path) {
        File file = new File(path);
        if (!file.isFile() || !file.canRead()) return "";

        StringBuilder result = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(file), StandardCharsets.UTF_8))) {
            char[] buffer = new char[1024];
            int remaining = 4096;
            while (remaining > 0) {
                int count = reader.read(buffer, 0, Math.min(buffer.length, remaining));
                if (count < 0) break;
                result.append(buffer, 0, count);
                remaining -= count;
            }
        } catch (Exception ignored) {
            return "";
        }
        return result.toString();
    }

    private static String readPanelProperties() {
        Process process = null;
        StringBuilder result = new StringBuilder();
        try {
            process = new ProcessBuilder("/system/bin/getprop")
                    .redirectErrorStream(true)
                    .start();
            boolean finished = process.waitFor(350, TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroy();
                return "";
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    int keyStart = line.indexOf('[');
                    int keyEnd = line.indexOf(']');
                    int valueStart = line.indexOf('[', keyEnd + 1);
                    int valueEnd = line.lastIndexOf(']');
                    if (keyStart < 0 || keyEnd <= keyStart || valueStart < 0 || valueEnd <= valueStart) continue;

                    String key = line.substring(keyStart + 1, keyEnd).trim().toLowerCase(Locale.ROOT);
                    if (!isPanelProperty(key)) continue;
                    String value = line.substring(valueStart + 1, valueEnd).trim();
                    if (!value.isEmpty()) result.append('\n').append(value);
                }
            }
        } catch (Exception ignored) {
            return "";
        } finally {
            if (process != null) process.destroy();
        }
        return result.toString();
    }

    private static boolean isPanelProperty(String key) {
        if (PANEL_PROPERTY_KEYS.contains(key)) return true;
        if (key.contains("density") || key.contains("brightness") || key.contains("refresh")) return false;
        return key.contains("panel") && (key.contains("display") || key.contains("vendor") || key.contains("boot"));
    }

    private static boolean containsAny(String value, String... needles) {
        for (String needle : needles) {
            if (value.contains(needle)) return true;
        }
        return false;
    }

    private static void applyWindowBackground(Activity activity) {
        Window window = activity.getWindow();
        if (window == null) return;
        int background = Ui.BG;
        window.setStatusBarColor(background);
        window.setNavigationBarColor(background);
        window.getDecorView().setBackgroundColor(background);
    }

    private static final class WindowBackgroundCallbacks implements Application.ActivityLifecycleCallbacks {
        @Override public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
            applyWindowBackground(activity);
        }

        @Override public void onActivityStarted(Activity activity) {}
        @Override public void onActivityResumed(Activity activity) {}
        @Override public void onActivityPaused(Activity activity) {}
        @Override public void onActivityStopped(Activity activity) {}
        @Override public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}
        @Override public void onActivityDestroyed(Activity activity) {}
    }
}
