package com.careychi.hrstrap;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;

public final class HeartRateApplication extends Application implements DefaultLifecycleObserver {
    @Override public void onCreate() {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate();
        PureBlackBackground.install(this);
        OledBurnInProtection.install(this);
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
    }

    @Override public void onStart(@NonNull LifecycleOwner owner) {
        AppVisibility.setForeground(true);
    }

    @Override public void onStop(@NonNull LifecycleOwner owner) {
        AppVisibility.setForeground(false);
    }
}
