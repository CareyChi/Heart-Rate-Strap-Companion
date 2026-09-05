package com.careychi.hrstrap.ui;

import android.app.Dialog;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.graphics.drawable.ColorDrawable;
import android.os.*;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.careychi.hrstrap.data.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

public final class HistoryActivity extends AppCompatActivity {
    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
    private LinearLayout list;
    private View root;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
        load();
    }

    @Override protected void onDestroy() {
        dbExecutor.shutdownNow();
        super.onDestroy();
    }

    private void buildUi() {
        LinearLayout page = Ui.column(this);
        page.setBackgroundColor(Ui.BG);
        page.setPadding(Ui.dp(this, 20), Ui.dp(this, 16), Ui.dp(this, 20), 0);
        root = page;
        LinearLayout top = Ui.row(this);
        TextView back = Ui.text(this, "‹", 36, Ui.TEXT);
        back.setGravity(Gravity.CENTER);
        back.setOnClickListener(v -> finish());
        top.addView(back, new LinearLayout.LayoutParams(Ui.dp(this, 44), Ui.dp(this, 48)));
        top.addView(Ui.title(this, "历史记录"));
        page.addView(top);

        ScrollView scroll = new ScrollView(this);
        list = Ui.column(this);
        list.setPadding(0, Ui.dp(this, 12), 0, Ui.dp(this, 30));
        scroll.addView(list, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        page.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        setContentView(page);
    }

    private void load() {
        dbExecutor.execute(() -> {
            List<RecordingSession> sessions = AppDatabase.get(getApplicationContext()).heartRateDao().getSessions();
            main.post(() -> render(sessions));
        });
    }

    private void render(List<RecordingSession> sessions) {
        list.removeAllViews();
        if (sessions.isEmpty()) {
            TextView empty = Ui.text(this, "还没有记录", 16, Ui.MUTED);
            empty.setGravity(Gravity.CENTER);
            list.addView(empty, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 180)));
            return;
        }
        for (RecordingSession session : sessions) list.addView(sessionCard(session));
    }

    private View sessionCard(RecordingSession s) {
        LinearLayout card = Ui.column(this);
        card.setBackground(Ui.rounded(Ui.SURFACE, 20, this));
        Ui.pad(card, 16);
        card.setClickable(true);
        card.setFocusable(true);

        LinearLayout header = Ui.row(this);
        TextView type = Ui.text(this, "CONTINUOUS".equals(s.type) ? "持续记录" : "骑行记录", 17, Ui.TEXT);
        type.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        header.addView(type, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        FixedSlotTextView date = new FixedSlotTextView(this, 12);
        date.setColor(Ui.MUTED);
        date.setValue(dateFormat.format(new Date(s.startTimeMs)));
        header.addView(date);
        card.addView(header);

        LinearLayout row = Ui.row(this);
        row.setPadding(0, Ui.dp(this, 16), 0, 0);
        row.addView(durationMetric(s.durationMs), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.3f));
        row.addView(bpmMetric("最大", s.maxBpm), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        row.addView(bpmMetric("平均", s.avgBpm), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        card.addView(row);
        card.setOnClickListener(v -> showDetails(s));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = Ui.dp(this, 12);
        card.setLayoutParams(lp);
        return card;
    }

    private View durationMetric(long ms) {
        LinearLayout box = Ui.column(this);
        box.addView(Ui.text(this, "总时长", 12, Ui.MUTED));
        FixedSlotTextView slots = new FixedSlotTextView(this, 15);
        slots.setValue(formatDuration(ms));
        box.addView(slots);
        return box;
    }

    private View bpmMetric(String label, int bpm) {
        LinearLayout box = Ui.column(this);
        box.addView(Ui.text(this, label + "心率", 12, Ui.MUTED));
        LinearLayout value = Ui.row(this);
        TripleDigitView digits = new TripleDigitView(this, 15);
        if (bpm > 0) digits.setValue(bpm); else digits.setUnavailable();
        value.addView(digits);
        value.addView(Ui.text(this, "bpm", 10, Ui.MUTED));
        box.addView(value);
        return box;
    }

    private void showDetails(RecordingSession s) {
        if (Build.VERSION.SDK_INT >= 31) {
            root.setRenderEffect(RenderEffect.createBlurEffect(18f, 18f, Shader.TileMode.CLAMP));
        }
        root.animate().alpha(0.62f).setDuration(180).start();

        Dialog dialog = new Dialog(this);
        LinearLayout panel = Ui.column(this);
        panel.setBackground(Ui.rounded(Ui.SURFACE_2, 28, this));
        panel.setPadding(Ui.dp(this, 20), Ui.dp(this, 20), Ui.dp(this, 20), Ui.dp(this, 18));

        TextView type = Ui.title(this, "CONTINUOUS".equals(s.type) ? "持续记录" : "骑行记录");
        panel.addView(type);
        FixedSlotTextView date = new FixedSlotTextView(this, 14);
        date.setColor(Ui.MUTED);
        date.setValue(dateFormat.format(new Date(s.startTimeMs)));
        panel.addView(date);

        LinearLayout metrics = Ui.row(this);
        metrics.setPadding(0, Ui.dp(this, 14), 0, Ui.dp(this, 14));
        metrics.addView(durationMetric(s.durationMs), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.2f));
        metrics.addView(bpmMetric("最大", s.maxBpm), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        metrics.addView(bpmMetric("平均", s.avgBpm), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        panel.addView(metrics);

        HeartRateChartView chart = new HeartRateChartView(this);
        chart.setBackground(Ui.rounded(Ui.SURFACE, 20, this));
        panel.addView(chart, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        TextView close = Ui.text(this, "关闭", 15, Ui.ACCENT);
        close.setGravity(Gravity.CENTER);
        close.setOnClickListener(v -> dialog.dismiss());
        panel.addView(close, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 52)));

        dialog.setContentView(panel);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
            window.setDimAmount(0.2f);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        }
        dialog.setOnShowListener(d -> {
            Window w = dialog.getWindow();
            if (w != null) w.setLayout((int) (getResources().getDisplayMetrics().widthPixels * 0.92f),
                    (int) (getResources().getDisplayMetrics().heightPixels * 0.78f));
            panel.setScaleX(0.92f); panel.setScaleY(0.92f); panel.setAlpha(0f);
            panel.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(220).start();
        });
        dialog.setOnDismissListener(d -> {
            if (Build.VERSION.SDK_INT >= 31) root.setRenderEffect(null);
            root.animate().alpha(1f).setDuration(180).start();
        });
        dialog.show();

        dbExecutor.execute(() -> {
            List<HeartRateSample> samples = AppDatabase.get(getApplicationContext()).heartRateDao().getSamples(s.id);
            ArrayList<HeartRateChartView.Point> points = new ArrayList<>(samples.size());
            for (HeartRateSample sample : samples) points.add(new HeartRateChartView.Point(sample.timestampMs, sample.bpm));
            main.post(() -> chart.setPoints(points));
        });
    }

    private static String formatDuration(long ms) {
        long totalSeconds = Math.max(0, ms / 1000);
        long h = totalSeconds / 3600;
        long m = (totalSeconds % 3600) / 60;
        long s = totalSeconds % 60;
        return String.format(Locale.US, "%02d:%02d:%02d", h, m, s);
    }
}
