package com.careychi.hrstrap.service;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.*;
import android.bluetooth.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import com.careychi.hrstrap.AppVisibility;
import com.careychi.hrstrap.HeartRateState;
import com.careychi.hrstrap.core.HeartRateAxis;
import com.careychi.hrstrap.core.HeartRateMeasurementParser;
import com.careychi.hrstrap.data.*;
import com.careychi.hrstrap.ui.*;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** BLE connection + 1 Hz recording foreground service. */
public final class HeartRateService extends Service implements AppVisibility.Listener {
    public static final String ACTION_CONNECT = "com.careychi.hrstrap.CONNECT";
    public static final String ACTION_START_RECORDING = "com.careychi.hrstrap.START_RECORDING";
    public static final String ACTION_RESUME_RECORDING = "com.careychi.hrstrap.RESUME_RECORDING";
    public static final String ACTION_STOP_RECORDING = "com.careychi.hrstrap.STOP_RECORDING";
    public static final String EXTRA_ADDRESS = "address";
    public static final String EXTRA_NAME = "name";
    public static final String EXTRA_SESSION_ID = "session_id";

    private static final UUID HR_SERVICE = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb");
    private static final UUID HR_MEASUREMENT = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb");
    private static final UUID CCCD = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private static final String CHANNEL_ID = "heart_rate_recording";
    private static final int NOTIFICATION_ID = 18013;
    private static final int OVERLAY_WIDTH_DP = 173;
    private static final int OVERLAY_HEIGHT_DP = 150;

    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();
    private BluetoothGatt gatt;
    private String requestedName = "心率带";
    private volatile int latestBpm;
    private volatile boolean recording;
    private volatile long sessionId;
    private volatile long startedAt;
    private long sampleSum;
    private int sampleCount;
    private int maxBpm;
    private long lastRecoverySaveAt;

    private WindowManager windowManager;
    private View overlay;
    private WindowManager.LayoutParams overlayParams;
    private TripleDigitView overlayBpm;
    private TripleDigitView overlayMax;
    private TripleDigitView overlayAvg;
    private TripleDigitView overlayZero;
    private MiniTrendView miniTrend;

    public static long activeSessionId() { return ActiveSessionHolder.id; }
    private static final class ActiveSessionHolder { static volatile long id; }

    @Override public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        AppVisibility.addListener(this);
        main.post(tick);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        ensureForeground();
        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case ACTION_CONNECT -> connect(intent.getStringExtra(EXTRA_ADDRESS), intent.getStringExtra(EXTRA_NAME));
                case ACTION_START_RECORDING -> startRecording();
                case ACTION_RESUME_RECORDING -> resumeRecording(intent.getLongExtra(EXTRA_SESSION_ID, 0));
                case ACTION_STOP_RECORDING -> stopRecording();
            }
        }
        return START_STICKY;
    }

    @Override public void onDestroy() {
        AppVisibility.removeListener(this);
        main.removeCallbacks(tick);
        hideOverlay();
        closeGatt();
        dbExecutor.shutdown();
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }

    @Override public void onAppForegroundChanged(boolean foreground) {
        main.post(this::syncOverlayVisibility);
    }

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (recording && sessionId > 0 && latestBpm > 0) {
                final int bpm = latestBpm;
                final long now = System.currentTimeMillis();
                sampleSum += bpm;
                sampleCount++;
                maxBpm = Math.max(maxBpm, bpm);
                int avg = (int) Math.round(sampleSum / (double) sampleCount);
                HeartRateState.get().updateRecording(true, startedAt, maxBpm, avg);
                dbExecutor.execute(() -> AppDatabase.get(getApplicationContext()).heartRateDao()
                        .insertSample(new HeartRateSample(sessionId, now, bpm)));
                updateOverlayValues(bpm, maxBpm, avg);
                if (RecordingRecovery.isEnabled(HeartRateService.this) && now - lastRecoverySaveAt >= 5000) {
                    lastRecoverySaveAt = now;
                    persistRecoveryProgress(now, maxBpm, avg);
                }
            }
            syncOverlayVisibility();
            main.postDelayed(this, 1000);
        }
    };

    private void startRecording() {
        if (recording || sessionId > 0) return;
        final long start = System.currentTimeMillis();
        sampleSum = 0;
        sampleCount = 0;
        maxBpm = 0;
        lastRecoverySaveAt = start;
        dbExecutor.execute(() -> {
            RecordingSession session = new RecordingSession("CONTINUOUS", start, 0, 0, 0, 0);
            long id = AppDatabase.get(getApplicationContext()).heartRateDao().insertSession(session);
            main.post(() -> {
                sessionId = id;
                ActiveSessionHolder.id = id;
                startedAt = start;
                recording = true;
                RecordingRecovery.markRecording(this, id, start);
                HeartRateState.get().updateRecording(true, start, 0, 0);
                syncOverlayVisibility();
            });
        });
    }

    private void resumeRecording(long id) {
        if (recording || sessionId > 0 || id <= 0) return;
        dbExecutor.execute(() -> {
            HeartRateDao dao = AppDatabase.get(getApplicationContext()).heartRateDao();
            RecordingSession session = dao.getSession(id);
            if (session == null) {
                RecordingRecovery.clearMarker(this);
                return;
            }
            List<HeartRateSample> samples = dao.getSamples(id);
            long sum = 0;
            int count = 0;
            int max = 0;
            for (HeartRateSample sample : samples) {
                if (sample.bpm <= 0) continue;
                sum += sample.bpm;
                count++;
                max = Math.max(max, sample.bpm);
            }
            final long restoredSum = sum;
            final int restoredCount = count;
            final int restoredMax = max;
            main.post(() -> {
                sessionId = id;
                ActiveSessionHolder.id = id;
                startedAt = session.startTimeMs;
                sampleSum = restoredSum;
                sampleCount = restoredCount;
                maxBpm = restoredMax;
                lastRecoverySaveAt = System.currentTimeMillis();
                recording = true;
                int avg = sampleCount == 0 ? 0 : (int) Math.round(sampleSum / (double) sampleCount);
                RecordingRecovery.markRecording(this, id, startedAt);
                HeartRateState.get().updateRecording(true, startedAt, maxBpm, avg);
                syncOverlayVisibility();
            });
        });
    }

    private void persistRecoveryProgress(long end, int max, int avg) {
        final long id = sessionId;
        final long start = startedAt;
        if (id <= 0 || start <= 0) return;
        dbExecutor.execute(() -> {
            RecordingSession session = AppDatabase.get(getApplicationContext()).heartRateDao().getSession(id);
            if (session == null || !recording || sessionId != id) return;
            session.endTimeMs = end;
            session.durationMs = Math.max(0, end - start);
            session.maxBpm = max;
            session.avgBpm = avg;
            AppDatabase.get(getApplicationContext()).heartRateDao().updateSession(session);
        });
    }

    private void stopRecording() {
        if (!recording || sessionId <= 0) return;
        final long id = sessionId;
        final long start = startedAt;
        final long end = System.currentTimeMillis();
        final int finalMax = maxBpm;
        final int finalAvg = sampleCount == 0 ? 0 : (int) Math.round(sampleSum / (double) sampleCount);
        recording = false;
        sessionId = 0;
        ActiveSessionHolder.id = 0;
        RecordingRecovery.clearMarker(this);
        HeartRateState.get().updateRecording(false, 0, finalMax, finalAvg);
        hideOverlay();
        dbExecutor.execute(() -> {
            HeartRateDao dao = AppDatabase.get(getApplicationContext()).heartRateDao();
            RecordingSession session = dao.getSession(id);
            if (session != null) {
                session.endTimeMs = end;
                session.durationMs = Math.max(0, end - start);
                session.maxBpm = finalMax;
                session.avgBpm = finalAvg;
                dao.updateSession(session);
            }
        });
    }

    @SuppressLint("MissingPermission")
    private void connect(String address, String name) {
        if (address == null || address.trim().isEmpty()) return;
        if (Build.VERSION.SDK_INT >= 31 && ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return;
        requestedName = (name == null || name.trim().isEmpty()) ? "心率带" : name;
        closeGatt();
        BluetoothManager manager = getSystemService(BluetoothManager.class);
        if (manager == null || manager.getAdapter() == null) return;
        BluetoothDevice device = manager.getAdapter().getRemoteDevice(address);
        gatt = device.connectGatt(this, false, callback, BluetoothDevice.TRANSPORT_LE);
    }

    @SuppressLint("MissingPermission")
    private void closeGatt() {
        if (gatt != null) {
            try { gatt.disconnect(); } catch (Exception ignored) {}
            try { gatt.close(); } catch (Exception ignored) {}
            gatt = null;
        }
    }

    private final BluetoothGattCallback callback = new BluetoothGattCallback() {
        @Override public void onConnectionStateChange(BluetoothGatt g, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                HeartRateState.get().updateConnection(true, requestedName);
                discover(g);
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                latestBpm = 0;
                HeartRateState.get().updateConnection(false, "未连接心率带");
            }
        }

        @SuppressLint("MissingPermission") private void discover(BluetoothGatt g) {
            try { g.discoverServices(); } catch (SecurityException ignored) {}
        }

        @Override public void onServicesDiscovered(BluetoothGatt g, int status) {
            if (status != BluetoothGatt.GATT_SUCCESS) return;
            enableMeasurement(g);
        }

        @Override public void onCharacteristicChanged(BluetoothGatt g, BluetoothGattCharacteristic characteristic, byte[] value) {
            if (HR_MEASUREMENT.equals(characteristic.getUuid())) consume(value);
        }

        @Deprecated
        @Override public void onCharacteristicChanged(BluetoothGatt g, BluetoothGattCharacteristic characteristic) {
            if (HR_MEASUREMENT.equals(characteristic.getUuid())) consume(characteristic.getValue());
        }
    };

    @SuppressLint("MissingPermission")
    private void enableMeasurement(BluetoothGatt g) {
        if (Build.VERSION.SDK_INT >= 31 && ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return;
        BluetoothGattService service = g.getService(HR_SERVICE);
        if (service == null) return;
        BluetoothGattCharacteristic c = service.getCharacteristic(HR_MEASUREMENT);
        if (c == null) return;
        g.setCharacteristicNotification(c, true);
        BluetoothGattDescriptor d = c.getDescriptor(CCCD);
        if (d == null) return;
        if (Build.VERSION.SDK_INT >= 33) g.writeDescriptor(d, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        else {
            d.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            g.writeDescriptor(d);
        }
    }

    private void consume(byte[] value) {
        try {
            int bpm = HeartRateMeasurementParser.parseBpm(value);
            if (bpm > 0 && bpm <= 999) {
                latestBpm = bpm;
                HeartRateState.get().updateBpm(bpm);
            }
        } catch (IllegalArgumentException ignored) {}
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "心率记录", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("保持心率带连接和持续记录");
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    private void ensureForeground() {
        Intent open = new Intent(this, com.careychi.hrstrap.ui.ContinuousRecordingActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 1, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .setContentTitle(recording ? "正在持续记录心率" : "心率带已启用")
                .setContentText(HeartRateState.get().snapshot().deviceName())
                .setOngoing(true)
                .setContentIntent(pi)
                .build();
        startForeground(NOTIFICATION_ID, notification);
    }

    private void syncOverlayVisibility() {
        boolean shouldShow = recording && !AppVisibility.isForeground() && Settings.canDrawOverlays(this);
        if (shouldShow) showOverlay(); else hideOverlay();
    }

    private void showOverlay() {
        if (overlay != null) return;
        windowManager = getSystemService(WindowManager.class);
        if (windowManager == null) return;
        LinearLayout root = Ui.column(this);
        Ui.pad(root, 12);
        root.setBackground(Ui.rounded(Ui.SURFACE, 18, this));
        root.setElevation(Ui.dp(this, 12));

        LinearLayout top = Ui.row(this);
        top.setGravity(Gravity.CENTER);
        overlayBpm = new TripleDigitView(this, 42);
        TextView bpm = Ui.text(this, "bpm", 16, Ui.TEXT);
        top.addView(overlayBpm);
        top.addView(bpm);
        root.addView(top, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout lower = Ui.row(this);
        FrameLayout axis = new FrameLayout(this);
        overlayMax = new TripleDigitView(this, 10);
        overlayAvg = new TripleDigitView(this, 10);
        overlayZero = new TripleDigitView(this, 10);
        FrameLayout.LayoutParams maxLp = new FrameLayout.LayoutParams(Ui.dp(this, 32), ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        FrameLayout.LayoutParams avgLp = new FrameLayout.LayoutParams(Ui.dp(this, 32), ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        FrameLayout.LayoutParams zeroLp = new FrameLayout.LayoutParams(Ui.dp(this, 32), ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        axis.addView(overlayMax, maxLp);
        axis.addView(overlayAvg, avgLp);
        axis.addView(overlayZero, zeroLp);
        miniTrend = new MiniTrendView(this);
        lower.addView(axis, new LinearLayout.LayoutParams(Ui.dp(this, 34), Ui.dp(this, 86)));
        lower.addView(miniTrend, new LinearLayout.LayoutParams(0, Ui.dp(this, 86), 1));
        root.addView(lower, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 86)));

        overlayParams = new WindowManager.LayoutParams(
                Ui.dp(this, OVERLAY_WIDTH_DP), Ui.dp(this, OVERLAY_HEIGHT_DP),
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        overlayParams.gravity = Gravity.TOP | Gravity.START;
        overlayParams.x = Ui.dp(this, 18);
        overlayParams.y = Ui.dp(this, 120);
        attachOverlayTouch(root);
        overlay = root;
        updateOverlayValues(latestBpm, maxBpm, sampleCount == 0 ? 0 : (int) Math.round(sampleSum / (double) sampleCount));
        try { windowManager.addView(root, overlayParams); } catch (Exception e) { overlay = null; }
    }

    private void hideOverlay() {
        if (overlay != null && windowManager != null) {
            try { windowManager.removeView(overlay); } catch (Exception ignored) {}
        }
        overlay = null;
        overlayBpm = overlayMax = overlayAvg = overlayZero = null;
        miniTrend = null;
    }

    private void updateOverlayValues(int bpm, int max, int avg) {
        if (overlayBpm == null) return;
        if (bpm > 0) overlayBpm.setValue(bpm); else overlayBpm.setUnavailable();
        int maxBand = Math.max(25, HeartRateAxis.ceil25(max));
        int avgBand = Math.max(25, HeartRateAxis.ceil25(avg));
        maxBand = Math.max(maxBand, avgBand + 25);
        overlayMax.setValue(maxBand);
        overlayAvg.setValue(avgBand);
        overlayZero.setValue(0);
        if (miniTrend != null && bpm > 0) miniTrend.addValue(bpm);
    }

    private void attachOverlayTouch(View root) {
        root.setOnTouchListener(new View.OnTouchListener() {
            float downRawX, downRawY;
            int startX, startY;
            boolean moved;
            long downAt;

            @Override public boolean onTouch(View v, android.view.MotionEvent event) {
                switch (event.getActionMasked()) {
                    case android.view.MotionEvent.ACTION_DOWN -> {
                        downRawX = event.getRawX();
                        downRawY = event.getRawY();
                        startX = overlayParams.x;
                        startY = overlayParams.y;
                        downAt = System.currentTimeMillis();
                        moved = false;
                        return true;
                    }
                    case android.view.MotionEvent.ACTION_MOVE -> {
                        float dx = event.getRawX() - downRawX;
                        float dy = event.getRawY() - downRawY;
                        if (Math.hypot(dx, dy) > Ui.dp(HeartRateService.this, 6)) moved = true;
                        if (moved && windowManager != null && overlay != null) {
                            int maxX = Math.max(0, getResources().getDisplayMetrics().widthPixels - Ui.dp(HeartRateService.this, OVERLAY_WIDTH_DP));
                            int maxY = Math.max(0, getResources().getDisplayMetrics().heightPixels - Ui.dp(HeartRateService.this, OVERLAY_HEIGHT_DP));
                            overlayParams.x = Math.max(0, Math.min(maxX, startX + Math.round(dx)));
                            overlayParams.y = Math.max(0, Math.min(maxY, startY + Math.round(dy)));
                            try { windowManager.updateViewLayout(overlay, overlayParams); } catch (Exception ignored) {}
                        }
                        return true;
                    }
                    case android.view.MotionEvent.ACTION_UP -> {
                        if (!moved && System.currentTimeMillis() - downAt < 600) {
                            Intent i = new Intent(HeartRateService.this, com.careychi.hrstrap.ui.ContinuousRecordingActivity.class)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(i);
                        }
                        return true;
                    }
                }
                return false;
            }
        });
    }
}
