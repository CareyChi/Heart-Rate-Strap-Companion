package com.careychi.hrstrap;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.Window;
import com.careychi.hrstrap.ui.Ui;
import java.util.WeakHashMap;

/** User-controlled pure-black background preference. No display-type detection is performed. */
public final class PureBlackBackground {
    private static final String PREFS = "display_style";
    private static final String KEY_ENABLED = "pure_black_background";
    private static final WeakHashMap<Activity, Boolean> activityPaletteStates = new WeakHashMap<>();
    private static boolean installed;

    private PureBlackBackground() {}

    /** Applies the saved preference at process startup and keeps window chrome in sync. */
    public static synchronized void install(Application application) {
        Ui.applyPureBlackBackground(isEnabled(application));
        if (!installed) {
            application.registerActivityLifecycleCallbacks(new WindowBackgroundCallbacks());
            installed = true;
        }
    }

    public static boolean isEnabled(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean(KEY_ENABLED, false);
    }

    public static void setEnabled(Context context, boolean enabled) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_ENABLED, enabled)
                .apply();
        Ui.applyPureBlackBackground(enabled);
        if (context instanceof Activity activity) applyWindowBackground(activity);
    }

    private static void applyWindowBackground(Activity activity) {
        Window window = activity.getWindow();
        if (window == null) return;
        int background = Ui.BG;
        window.setStatusBarColor(background);
        window.setNavigationBarColor(background);
        window.getDecorView().setBackgroundColor(background);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.getDecorView().setForceDarkAllowed(false);
        }
    }

    private static final class WindowBackgroundCallbacks implements Application.ActivityLifecycleCallbacks {
        @Override public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
            activityPaletteStates.put(activity, isEnabled(activity));
            applyWindowBackground(activity);
        }

        @Override public void onActivityResumed(Activity activity) {
            boolean enabled = isEnabled(activity);
            Boolean createdWith = activityPaletteStates.get(activity);
            if (createdWith == null) {
                activityPaletteStates.put(activity, enabled);
                applyWindowBackground(activity);
                return;
            }
            if (createdWith.booleanValue() != enabled) {
                activityPaletteStates.put(activity, enabled);
                activity.recreate();
                return;
            }
            applyWindowBackground(activity);
        }

        @Override public void onActivityDestroyed(Activity activity) {
            activityPaletteStates.remove(activity);
        }

        @Override public void onActivityStarted(Activity activity) {}
        @Override public void onActivityPaused(Activity activity) {}
        @Override public void onActivityStopped(Activity activity) {}
        @Override public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}
    }
}
