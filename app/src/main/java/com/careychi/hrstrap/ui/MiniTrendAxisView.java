package com.careychi.hrstrap.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.View;
import java.lang.ref.WeakReference;

/** Three-label Y axis for the overlay trend. Labels share the exact plot geometry with MiniTrendView. */
public final class MiniTrendAxisView extends View {
    public static final int WIDTH_DP = 20;
    public static final int GAP_DP = 2;

    private static WeakReference<MiniTrendAxisView> activeAxis = new WeakReference<>(null);

    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int maxBand = 100;
    private int midBand = 50;
    private int minBand = 20;

    public MiniTrendAxisView(Context context) {
        super(context);
        text.setColor(Ui.MUTED);
        text.setTextSize(Ui.dp(context, 10));
        text.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        text.setTextAlign(Paint.Align.RIGHT);
        activeAxis = new WeakReference<>(this);
    }

    /** Kept for HeartRateService source compatibility; the visible 60-second trend owns the scale. */
    public void setBands(int ignoredMaxBand, int ignoredAvgBand) {
        // Intentionally ignored in V0.0.2B.
    }

    static void publishBands(int maxBand, int midBand, int minBand) {
        MiniTrendAxisView axis = activeAxis.get();
        if (axis == null) return;
        axis.maxBand = Math.max(100, maxBand);
        axis.midBand = Math.max(0, Math.min(axis.maxBand, midBand));
        axis.minBand = Math.max(0, Math.min(axis.midBand, minBand));
        axis.invalidate();
    }

    @Override protected void onDetachedFromWindow() {
        if (activeAxis.get() == this) activeAxis.clear();
        super.onDetachedFromWindow();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawBand(canvas, maxBand);
        drawBand(canvas, midBand);
        drawBand(canvas, minBand);
    }

    private void drawBand(Canvas canvas, int value) {
        float y = MiniTrendView.yForValue(
                getContext(), getHeight(), value, minBand, midBand, maxBand);
        Paint.FontMetrics fm = text.getFontMetrics();
        float baseline = y - (fm.ascent + fm.descent) / 2f;
        float minBaseline = -fm.ascent;
        float maxBaseline = getHeight() - fm.descent;
        baseline = Math.max(minBaseline, Math.min(maxBaseline, baseline));
        canvas.drawText(Integer.toString(value), getWidth() - Ui.dp(getContext(), 2), baseline, text);
    }
}
