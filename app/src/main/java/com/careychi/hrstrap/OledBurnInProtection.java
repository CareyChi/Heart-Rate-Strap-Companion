package com.careychi.hrstrap;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import java.lang.ref.WeakReference;

/**
 * Opt-in OLED burn-in mitigation for app activities.
 *
 * Static UI is shifted through a small 2 dp, nine-position pattern once per minute so the same
 * OLED sub-pixels are not driven continuously by bright text, icons, chart guides and borders.
 * The movement is absolute rather than cumulative and is reset immediately when protection is
 * disabled or the activity leaves the foreground.
 */
public final class OledBurnInProtection implements Application.ActivityLifecycleCallbacks {
    private static final String PREFS = "display_protection";
    private static final String KEY_ENABLED = "oled_burn_in_enabled";
    private static final long SHIFT_INTERVAL_MS = 60_000L;
    private static final float SHIFT_DP = 2f;
    private static final int[][] OFFSETS = {
            {0, 0}, {1, 0}, {0, 1}, {-1, 0}, {0, -1},
            {1, 1}, {-1, 1}, {-1, -1}, {1, -1}
    };

    private static volatile OledBurnInProtection instance;

    private final Handler main = new Handler(Looper.getMainLooper());
    private WeakReference<Activity> resumedActivity = new WeakReference<>(null);
    private View shiftedView;
    private int offsetIndex;

    private OledBurnInProtection() {}

    public static synchronized void install(Application application) {
        if (instance != null) return;
        OledBurnInProtection protection = new OledBurnInProtection();
        instance = protection;
        application.registerActivityLifecycleCallbacks(protection);
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
        OledBurnInProtection current = instance;
        if (current != null) current.refresh();
    }

    private void refresh() {
        main.post(() -> {
            Activity activity = resumedActivity.get();
            main.removeCallbacks(shiftRunnable);
            if (activity == null || !isEnabled(activity)) {
                resetShift();
                return;
            }
            applyNextShift(activity);
        });
    }

    private final Runnable shiftRunnable = new Runnable() {
        @Override public void run() {
            Activity activity = resumedActivity.get();
            if (activity == null || !isEnabled(activity)) {
                resetShift();
                return;
            }
            applyNextShift(activity);
        }
    };

    private void applyNextShift(Activity activity) {
        View target = activityShiftTarget(activity);
        if (target == null) return;
        if (shiftedView != null && shiftedView != target) resetShift();
        shiftedView = target;

        int px = Math.max(1, Math.round(SHIFT_DP * activity.getResources().getDisplayMetrics().density));
        int[] offset = OFFSETS[offsetIndex % OFFSETS.length];
        offsetIndex = (offsetIndex + 1) % OFFSETS.length;
        target.animate()
                .translationX(offset[0] * px)
                .translationY(offset[1] * px)
                .setDuration(500L)
                .start();
        main.removeCallbacks(shiftRunnable);
        main.postDelayed(shiftRunnable, SHIFT_INTERVAL_MS);
    }

    private static View activityShiftTarget(Activity activity) {
        View content = activity.findViewById(android.R.id.content);
        if (content instanceof ViewGroup group && group.getChildCount() > 0) {
            return group.getChildAt(0);
        }
        return content;
    }

    private void resetShift() {
        if (shiftedView != null) {
            shiftedView.animate().cancel();
            shiftedView.setTranslationX(0f);
            shiftedView.setTranslationY(0f);
            shiftedView = null;
        }
    }

    @Override public void onActivityResumed(Activity activity) {
        resumedActivity = new WeakReference<>(activity);
        refresh();
    }

    @Override public void onActivityPaused(Activity activity) {
        Activity current = resumedActivity.get();
        if (current != activity) return;
        main.removeCallbacks(shiftRunnable);
        resetShift();
        resumedActivity = new WeakReference<>(null);
    }

    @Override public void onActivityCreated(Activity activity, Bundle savedInstanceState) {}
    @Override public void onActivityStarted(Activity activity) {}
    @Override public void onActivityStopped(Activity activity) {}
    @Override public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}
    @Override public void onActivityDestroyed(Activity activity) {}
}
