package com.careychi.hrstrap.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.View;

/** Two-label Y axis for the overlay trend. Labels share the exact plot geometry with MiniTrendView. */
public final class MiniTrendAxisView extends View {
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int maxBand = 25;
    private int avgBand;

    public MiniTrendAxisView(Context context) {
        super(context);
        text.setColor(Ui.MUTED);
        text.setTextSize(Ui.dp(context, 10));
        text.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        text.setTextAlign(Paint.Align.RIGHT);
    }

    public void setBands(int maxBand, int avgBand) {
        this.maxBand = Math.max(25, maxBand);
        this.avgBand = Math.max(0, Math.min(this.maxBand, avgBand));
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawBand(canvas, maxBand);
        drawBand(canvas, avgBand);
    }

    private void drawBand(Canvas canvas, int value) {
        float y = MiniTrendView.yForBand(getContext(), getHeight(), value, maxBand);
        Paint.FontMetrics fm = text.getFontMetrics();
        float baseline = y - (fm.ascent + fm.descent) / 2f;
        float minBaseline = -fm.ascent;
        float maxBaseline = getHeight() - fm.descent;
        baseline = Math.max(minBaseline, Math.min(maxBaseline, baseline));
        canvas.drawText(Integer.toString(value), getWidth() - Ui.dp(getContext(), 2), baseline, text);
    }
}
