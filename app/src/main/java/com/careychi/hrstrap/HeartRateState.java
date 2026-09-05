package com.careychi.hrstrap;

import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.CopyOnWriteArrayList;

/** Process-local observable state. Persistent history lives in Room. */
public final class HeartRateState {
    public interface Listener { void onState(State state); }

    public record State(boolean connected, String deviceName, int latestBpm, boolean recording,
                        long recordingStartedAtMs, int maxBpm, int avgBpm) {}

    private static final HeartRateState INSTANCE = new HeartRateState();
    public static HeartRateState get() { return INSTANCE; }

    private final Handler main = new Handler(Looper.getMainLooper());
    private final CopyOnWriteArrayList<Listener> listeners = new CopyOnWriteArrayList<>();
    private volatile State state = new State(false, "未连接心率带", 0, false, 0, 0, 0);

    private HeartRateState() {}

    public State snapshot() { return state; }

    public void addListener(Listener listener) {
        listeners.addIfAbsent(listener);
        main.post(() -> listener.onState(state));
    }

    public void removeListener(Listener listener) { listeners.remove(listener); }

    public void updateConnection(boolean connected, String name) {
        State s = state;
        set(new State(connected, name == null ? "心率带" : name, connected ? s.latestBpm : 0,
                s.recording, s.recordingStartedAtMs, s.maxBpm, s.avgBpm));
    }

    public void updateBpm(int bpm) {
        State s = state;
        set(new State(s.connected, s.deviceName, bpm, s.recording, s.recordingStartedAtMs,
                s.maxBpm, s.avgBpm));
    }

    public void updateRecording(boolean recording, long startedAt, int max, int avg) {
        State s = state;
        set(new State(s.connected, s.deviceName, s.latestBpm, recording, startedAt, max, avg));
    }

    private void set(State next) {
        state = next;
        main.post(() -> {
            for (Listener listener : listeners) listener.onState(next);
        });
    }
}
