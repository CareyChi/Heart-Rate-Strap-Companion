package com.careychi.hrstrap;

import java.util.concurrent.CopyOnWriteArrayList;

public final class AppVisibility {
    public interface Listener { void onAppForegroundChanged(boolean foreground); }
    private static final CopyOnWriteArrayList<Listener> LISTENERS = new CopyOnWriteArrayList<>();
    private static volatile boolean foreground = true;
    private AppVisibility() {}

    public static boolean isForeground() { return foreground; }
    public static void addListener(Listener l) { LISTENERS.addIfAbsent(l); }
    public static void removeListener(Listener l) { LISTENERS.remove(l); }
    public static void setForeground(boolean value) {
        foreground = value;
        for (Listener l : LISTENERS) l.onAppForegroundChanged(value);
    }
}
