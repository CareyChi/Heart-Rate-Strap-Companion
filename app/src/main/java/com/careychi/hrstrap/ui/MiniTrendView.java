package com.careychi.hrstrap.ui;

import android.content.Context;
import android.graphics.*;
import android.view.View;
import java.util.ArrayDeque;
import java.util.Deque;

/** Compact 60-second trend used by the system overlay. */
public final class MiniTrendView extends View {
    public static final float PLOT_INSET_DP = 6f;

    private final Deque<Integer> values = new ArrayDeque<>();
    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint guide = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final Path fillPath = new Path();
    private int maxBand = 25;
    private int avgBand;

    public MiniTrendView(Context context) {
        super(context);
        line.setColor(Ui.ACCENT);
        line.setStrokeWidth(Ui.dp(context, 1.5f));
        line.setStyle(Paint.Style.STROKE);
        guide.setColor(Color.argb(100, 157, 178, 170));
        guide.setStrokeWidth(Ui.dp(context, 1));
        guide.setPathEffect(new DashPathEffect(new float[]{Ui.dp(context, 5), Ui.dp(context, 5)}, 0));
    }

    public void setBands(int maxBand, int avgBand) {
        this.maxBand = Math.max(25, maxBand);
        this.avgBand = Math.max(0, Math.min(this.maxBand, avgBand));
        invalidate();
    }

    public void addValue(int bpm) {
        if (bpm <= 0) return;
        values.addLast(bpm);
        while (values.size() > 60) values.removeFirst();
        invalidate();
    }

    public static float yForBand(Context context, int height, int value, int maxBand) {
        float top = Ui.dp(context, PLOT_INSET_DP);
        float bottom = height - Ui.dp(context, PLOT_INSET_DP);
        int safeMax = Math.max(25, maxBand);
        int clamped = Math.max(0, Math.min(safeMax, value));
        return bottom - (bottom - top) * clamped / (float) safeMax;
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float bottom = getHeight() - Ui.dp(getContext(), PLOT_INSET_DP);
        float avgY = yForBand(getContext(), getHeight(), avgBand, maxBand);
        canvas.drawLine(0, avgY, getWidth(), avgY, guide);
        if (values.isEmpty()) return;

        path.reset();
        fillPath.reset();
        int i = 0;
        int n = Math.max(2, values.size());
        float lastX = 0;
        for (int v : values) {
            float x = getWidth() * i / (float) (n - 1);
            float y = yForBand(getContext(), getHeight(), v, maxBand);
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
        float top = Ui.dp(getContext(), PLOT_INSET_DP);
        fill.setShader(new LinearGradient(0, top, 0, bottom,
                Color.argb(90, 72, 240, 164), Color.TRANSPARENT, Shader.TileMode.CLAMP));
        canvas.drawPath(fillPath, fill);
        fill.setShader(null);
    }
}
