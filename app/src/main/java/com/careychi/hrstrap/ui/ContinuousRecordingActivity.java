package com.careychi.hrstrap.ui;

import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.careychi.hrstrap.HeartRateState;
import com.careychi.hrstrap.data.*;
import com.careychi.hrstrap.service.HeartRateService;
import com.google.android.material.button.MaterialButton;
import java.util.*;
import java.util.concurrent.*;

public final class ContinuousRecordingActivity extends AppCompatActivity implements HeartRateState.Listener {
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();
    private TripleDigitView bpmDigits;
    private TripleDigitView maxDigits;
    private TripleDigitView avgDigits;
    private TextView deviceName;
    private HeartRateChartView chart;
    private MaterialButton stopButton;
    private long loadedSessionId;
    private long lastLoadedTimestamp;
    private boolean queryInFlight;
    private boolean everRecording;
    private long pressStartedAt;
    private boolean stopTriggered;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
    }

    @Override protected void onStart() {
        super.onStart();
        HeartRateState.get().addListener(this);
        main.post(chartTicker);
    }

    @Override protected void onStop() {
        HeartRateState.get().removeListener(this);
        main.removeCallbacks(chartTicker);
        main.removeCallbacks(pressProgress);
        super.onStop();
    }

    @Override protected void onDestroy() {
        dbExecutor.shutdownNow();
        super.onDestroy();
    }

    private void buildUi() {
        LinearLayout root = Ui.column(this);
        root.setBackgroundColor(Ui.BG);
        root.setPadding(Ui.dp(this, 20), Ui.dp(this, 16), Ui.dp(this, 20), Ui.dp(this, 18));

        LinearLayout top = Ui.row(this);
        TextView back = Ui.text(this, "‹", 36, Ui.TEXT);
        back.setGravity(Gravity.CENTER);
        back.setOnClickListener(v -> finish());
        top.addView(back, new LinearLayout.LayoutParams(Ui.dp(this, 44), Ui.dp(this, 48)));
        LinearLayout names = Ui.column(this);
        TextView title = Ui.text(this, "持续记录", 22, Ui.TEXT);
        title.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        deviceName = Ui.text(this, "心率带", 13, Ui.MUTED);
        names.addView(title); names.addView(deviceName);
        top.addView(names, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        root.addView(top);

        LinearLayout hero = Ui.row(this);
        hero.setGravity(Gravity.CENTER);
        hero.setBackground(Ui.rounded(Ui.SURFACE, 28, this));
        bpmDigits = new TripleDigitView(this, 64);
        bpmDigits.setUnavailable();
        hero.addView(bpmDigits);
        TextView bpm = Ui.text(this, "bpm", 18, Ui.MUTED);
        hero.addView(bpm);
        root.addView(hero, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 180)));

        LinearLayout metrics = Ui.row(this);
        metrics.setGravity(Gravity.CENTER);
        metrics.addView(metric("最大", true));
        Space metricGap = new Space(this);
        metrics.addView(metricGap, new LinearLayout.LayoutParams(Ui.dp(this, 28), 1));
        metrics.addView(metric("平均", false));
        root.addView(metrics, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 54)));

        chart = new HeartRateChartView(this);
        chart.setBackground(Ui.rounded(Ui.SURFACE, 22, this));
        root.addView(chart, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        Space gap = new Space(this);
        root.addView(gap, new LinearLayout.LayoutParams(1, Ui.dp(this, 18)));
        stopButton = new MaterialButton(this);
        stopButton.setText("长按停止");
        stopButton.setTextColor(Ui.TEXT);
        stopButton.setTextSize(16);
        stopButton.setCornerRadius(Ui.dp(this, 18));
        stopButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Ui.SURFACE_2));
        stopButton.setStrokeColor(android.content.res.ColorStateList.valueOf(Ui.ACCENT));
        stopButton.setStrokeWidth(Ui.dp(this, 1));
        attachHoldToStop();
        root.addView(stopButton, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 60)));
        setContentView(root);
    }

    private View metric(String label, boolean maximum) {
        LinearLayout box = Ui.row(this);
        TextView labelView = Ui.text(this, label + " ", 13, Ui.MUTED);
        box.addView(labelView);
        TripleDigitView digits = new TripleDigitView(this, 16);
        digits.setUnavailable();
        if (maximum) maxDigits = digits; else avgDigits = digits;
        box.addView(digits);
        box.addView(Ui.text(this, " bpm", 11, Ui.MUTED));
        return box;
    }

    private void attachHoldToStop() {
        stopButton.setOnTouchListener((v, e) -> {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN -> {
                    if (!HeartRateState.get().snapshot().recording()) return true;
                    pressStartedAt = SystemClock.uptimeMillis();
                    stopTriggered = false;
                    main.post(pressProgress);
                    return true;
                }
                case MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    main.removeCallbacks(pressProgress);
                    if (!stopTriggered) stopButton.setText("长按停止");
                    return true;
                }
            }
            return true;
        });
    }

    private final Runnable pressProgress = new Runnable() {
        @Override public void run() {
            long elapsed = SystemClock.uptimeMillis() - pressStartedAt;
            stopButton.setText(elapsed < 450 ? "继续按住以停止" : "即将停止…");
            if (elapsed >= 900 && !stopTriggered) {
                stopTriggered = true;
                stopButton.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                ContextCompat.startForegroundService(ContinuousRecordingActivity.this,
                        new Intent(ContinuousRecordingActivity.this, HeartRateService.class).setAction(HeartRateService.ACTION_STOP_RECORDING));
                stopButton.setText("已停止");
                main.postDelayed(ContinuousRecordingActivity.this::finish, 300);
                return;
            }
            main.postDelayed(this, 45);
        }
    };

    private final Runnable chartTicker = new Runnable() {
        @Override public void run() {
            refreshChart();
            main.postDelayed(this, 1000);
        }
    };

    private void refreshChart() {
        long id = HeartRateService.activeSessionId();
        if (id <= 0 || queryInFlight) return;
        if (id != loadedSessionId) {
            loadedSessionId = id;
            lastLoadedTimestamp = 0;
            chart.setPoints(Collections.emptyList());
        }
        queryInFlight = true;
        long from = lastLoadedTimestamp == 0 ? 0 : lastLoadedTimestamp + 1;
        dbExecutor.execute(() -> {
            List<HeartRateSample> samples = from == 0
                    ? AppDatabase.get(getApplicationContext()).heartRateDao().getSamples(id)
                    : AppDatabase.get(getApplicationContext()).heartRateDao().getSamplesSince(id, from);
            main.post(() -> {
                if (id == loadedSessionId && !samples.isEmpty()) {
                    ArrayList<HeartRateChartView.Point> existing = new ArrayList<>();
                    if (from == 0) {
                        for (HeartRateSample s : samples) existing.add(new HeartRateChartView.Point(s.timestampMs, s.bpm));
                        chart.setPoints(existing);
                    } else {
                        for (HeartRateSample s : samples) chart.append(new HeartRateChartView.Point(s.timestampMs, s.bpm));
                    }
                    lastLoadedTimestamp = samples.get(samples.size() - 1).timestampMs;
                }
                queryInFlight = false;
            });
        });
    }

    @Override public void onState(HeartRateState.State s) {
        deviceName.setText(s.deviceName());
        if (s.latestBpm() > 0) bpmDigits.setValue(s.latestBpm()); else bpmDigits.setUnavailable();
        if (s.maxBpm() > 0) {
            maxDigits.setValue(s.maxBpm());
            avgDigits.setValue(s.avgBpm());
        } else {
            maxDigits.setUnavailable();
            avgDigits.setUnavailable();
        }
        if (s.recording()) everRecording = true;
        if (everRecording && !s.recording() && !stopTriggered) finish();
    }
}
