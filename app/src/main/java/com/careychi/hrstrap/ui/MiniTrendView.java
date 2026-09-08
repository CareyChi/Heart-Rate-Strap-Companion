package com.careychi.hrstrap.ui;

import android.content.Context;
import android.graphics.*;
import android.view.View;
import com.careychi.hrstrap.core.HeartRateAxis;
import java.util.ArrayDeque;
import java.util.Deque;

/** Compact 60-second trend used by the system overlay. */
public final class MiniTrendView extends View {
    public static final float PLOT_INSET_DP = 6f;
    private static final float MID_HEIGHT_FROM_BOTTOM = 0.40f;

    private final Deque<Integer> values = new ArrayDeque<>();
    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint guide = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final Path fillPath = new Path();
    private int maxBand = 100;
    private int midBand = 50;
    private int minBand = 20;

    public MiniTrendView(Context context) {
        super(context);
        line.setColor(Ui.ACCENT);
        line.setStrokeWidth(Ui.dp(context, 1.5f));
        line.setStyle(Paint.Style.STROKE);
        guide.setColor(Color.argb(100, 157, 178, 170));
        guide.setStrokeWidth(Ui.dp(context, 1));
        guide.setPathEffect(new DashPathEffect(new float[]{Ui.dp(context, 5), Ui.dp(context, 5)}, 0));
        MiniTrendAxisView.publishBands(maxBand, midBand, minBand);
    }

    /** Kept for HeartRateService source compatibility; the visible 60-second trend owns the scale. */
    public void setBands(int ignoredMaxBand, int ignoredAvgBand) {
        // Intentionally ignored in V0.0.2B.
    }

    public void addValue(int bpm) {
        if (bpm <= 0) return;
        values.addLast(bpm);
        while (values.size() > 60) values.removeFirst();

        int recentMax = 0;
        for (int value : values) recentMax = Math.max(recentMax, value);
        HeartRateAxis.OverlayScale scale = HeartRateAxis.forOverlayRecent(recentMax);
        maxBand = scale.maxBand();
        midBand = scale.midBand();
        minBand = scale.minBand();
        MiniTrendAxisView.publishBands(maxBand, midBand, minBand);
        invalidate();
    }

    /**
     * Piecewise mapping required by the V0.0.2B overlay specification:
     * min -> bottom, mid -> 40% up from bottom, max -> top.
     */
    public static float yForValue(
            Context context, int height, int value, int minBand, int midBand, int maxBand) {
        float top = Ui.dp(context, PLOT_INSET_DP);
        float bottom = height - Ui.dp(context, PLOT_INSET_DP);
        float plotHeight = Math.max(1f, bottom - top);
        float midY = bottom - plotHeight * MID_HEIGHT_FROM_BOTTOM;

        int safeMin = Math.max(0, minBand);
        int safeMid = Math.max(safeMin + 1, midBand);
        int safeMax = Math.max(safeMid + 1, maxBand);
        int clamped = Math.max(safeMin, Math.min(safeMax, value));

        if (clamped <= safeMid) {
            float ratio = (clamped - safeMin) / (float) (safeMid - safeMin);
            return bottom - (bottom - midY) * ratio;
        }
        float ratio = (clamped - safeMid) / (float) (safeMax - safeMid);
        return midY - (midY - top) * ratio;
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float top = Ui.dp(getContext(), PLOT_INSET_DP);
        float bottom = getHeight() - Ui.dp(getContext(), PLOT_INSET_DP);
        float midY = yForValue(getContext(), getHeight(), midBand, minBand, midBand, maxBand);

        // Only the middle value gets a horizontal dashed guide. Max/min rows intentionally have none.
        canvas.drawLine(0, midY, getWidth(), midY, guide);
        if (values.isEmpty()) return;

        path.reset();
        fillPath.reset();
        int i = 0;
        int n = Math.max(2, values.size());
        float lastX = 0;
        for (int v : values) {
            float x = getWidth() * i / (float) (n - 1);
            float y = yForValue(getContext(), getHeight(), v, minBand, midBand, maxBand);
            if (i == 0) {
                path.moveTo(x, y);
                fillPath.moveTo(x, bottom);
                fillPath.lineTo(x, y);
            } else {
                path.lineTo(x, y);
                fillPath.lineTo(x, y);
            }
            lastX = x;
            i++;
        }
        canvas.drawPath(path, line);
        fillPath.lineTo(lastX, bottom);
        fillPath.close();
        fill.setShader(new LinearGradient(0, top, 0, bottom,
                Color.argb(90, 72, 240, 164), Color.TRANSPARENT, Shader.TileMode.CLAMP));
        canvas.drawPath(fillPath, fill);
        fill.setShader(null);
    }
}
