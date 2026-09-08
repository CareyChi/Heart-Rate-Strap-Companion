package com.careychi.hrstrap.data;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.List;

/** Persistent crash-recovery marker kept outside the recording tables. */
public final class RecordingRecovery {
    private static final String PREFS = "recording_recovery";
    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_RECORDING = "recording";
    private static final String KEY_SESSION_ID = "session_id";
    private static final String KEY_STARTED_AT = "started_at";

    private RecordingRecovery() {}

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static boolean isEnabled(Context context) {
        return prefs(context).getBoolean(KEY_ENABLED, false);
    }

    public static void setEnabled(Context context, boolean enabled) {
        SharedPreferences p = prefs(context);
        p.edit().putBoolean(KEY_ENABLED, enabled).apply();
        // Changing the setting applies to the next recording. An already-protected recording
        // keeps its marker until the user stops, saves, deletes, or resumes it.
        if (!enabled && !isRecordingMarked(context)) clearMarker(context);
    }

    public static boolean isRecordingMarked(Context context) {
        SharedPreferences p = prefs(context);
        return p.getBoolean(KEY_RECORDING, false) && p.getLong(KEY_SESSION_ID, 0) > 0;
    }

    public static boolean hasInterruptedRecording(Context context) {
        return isRecordingMarked(context);
    }

    public static long sessionId(Context context) {
        return prefs(context).getLong(KEY_SESSION_ID, 0);
    }

    public static long startedAt(Context context) {
        return prefs(context).getLong(KEY_STARTED_AT, 0);
    }

    public static void markRecording(Context context, long sessionId, long startedAt) {
        if (sessionId <= 0) return;
        prefs(context).edit()
                .putBoolean(KEY_RECORDING, true)
                .putLong(KEY_SESSION_ID, sessionId)
                .putLong(KEY_STARTED_AT, startedAt)
                .apply();
    }

    public static void clearMarker(Context context) {
        prefs(context).edit()
                .putBoolean(KEY_RECORDING, false)
                .remove(KEY_SESSION_ID)
                .remove(KEY_STARTED_AT)
                .apply();
    }

    public static void finalizeSession(Context context, long sessionId) {
        if (sessionId <= 0) {
            clearMarker(context);
            return;
        }
        HeartRateDao dao = AppDatabase.get(context).heartRateDao();
        RecordingSession session = dao.getSession(sessionId);
        if (session == null) {
            clearMarker(context);
            return;
        }
        List<HeartRateSample> samples = dao.getSamples(sessionId);
        long sum = 0;
        int count = 0;
        int max = 0;
        long end = session.startTimeMs;
        for (HeartRateSample sample : samples) {
            if (sample.bpm > 0) {
                sum += sample.bpm;
                count++;
                max = Math.max(max, sample.bpm);
            }
            end = Math.max(end, sample.timestampMs);
        }
        session.endTimeMs = end;
        session.durationMs = Math.max(0, end - session.startTimeMs);
        session.maxBpm = max;
        session.avgBpm = count == 0 ? 0 : (int) Math.round(sum / (double) count);
        dao.updateSession(session);
        clearMarker(context);
    }

    public static void deleteInterruptedSession(Context context, long sessionId) {
        if (sessionId > 0) AppDatabase.get(context).heartRateDao().deleteSession(sessionId);
        clearMarker(context);
    }
}
