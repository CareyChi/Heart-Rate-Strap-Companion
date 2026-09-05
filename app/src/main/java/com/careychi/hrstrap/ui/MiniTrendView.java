package com.careychi.hrstrap.ui;

import android.content.Context;
import android.graphics.*;
import android.view.View;
import com.careychi.hrstrap.core.HeartRateAxis;
import java.util.ArrayDeque;
import java.util.Deque;

/** Compact 60-second trend used by the system overlay. */
public final class MiniTrendView extends View {
    private final Deque<Integer> values = new ArrayDeque<>();
    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint guide = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final Path fillPath = new Path();

    public MiniTrendView(Context context) {
        super(context);
        line.setColor(Ui.ACCENT);
        line.setStrokeWidth(Ui.dp(context, 1.5f));
        line.setStyle(Paint.Style.STROKE);
        guide.setColor(Color.argb(100, 157, 178, 170));
        guide.setStrokeWidth(Ui.dp(context, 1));
        guide.setPathEffect(new DashPathEffect(new float[]{Ui.dp(context, 5), Ui.dp(context, 5)}, 0));
    }

    public void addValue(int bpm) {
        if (bpm <= 0) return;
        values.addLast(bpm);
        while (values.size() > 60) values.removeFirst();
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (values.isEmpty()) return;
        int max = 0, sum = 0;
        for (int v : values) { max = Math.max(max, v); sum += v; }
        int avg = Math.round(sum / (float) values.size());
        int maxBand = Math.max(25, HeartRateAxis.ceil25(max));
        int avgBand = Math.max(25, HeartRateAxis.ceil25(avg));
        maxBand = Math.max(maxBand, avgBand + 25);

        float top = Ui.dp(getContext(), 4), bottom = getHeight() - Ui.dp(getContext(), 4);
        float avgY = bottom - (bottom - top) * avgBand / (float) maxBand;
        canvas.drawLine(0, avgY, getWidth(), avgY, guide);

        path.reset(); fillPath.reset();
        int i = 0, n = Math.max(2, values.size());
        float lastX = 0;
        for (int v : values) {
            float x = getWidth() * i / (float) (n - 1);
            float y = bottom - (bottom - top) * Math.min(v, maxBand) / (float) maxBand;
            if (i == 0) { path.moveTo(x, y); fillPath.moveTo(x, bottom); fillPath.lineTo(x, y); }
            else { path.lineTo(x, y); fillPath.lineTo(x, y); }
            lastX = x; i++;
        }
        canvas.drawPath(path, line);
        fillPath.lineTo(lastX, bottom); fillPath.close();
        fill.setShader(new LinearGradient(0, top, 0, bottom, Color.argb(90,72,240,164), Color.TRANSPARENT, Shader.TileMode.CLAMP));
        canvas.drawPath(fillPath, fill);
        fill.setShader(null);
    }
}
