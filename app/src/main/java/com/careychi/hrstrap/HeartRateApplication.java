package com.careychi.hrstrap;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;

public final class HeartRateApplication extends Application implements DefaultLifecycleObserver {
    @Override public void onCreate() {
        super.onCreate();
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
    }

    @Override public void onStart(@NonNull LifecycleOwner owner) {
        AppVisibility.setForeground(true);
    }

    @Override public void onStop(@NonNull LifecycleOwner owner) {
        AppVisibility.setForeground(false);
    }
}
