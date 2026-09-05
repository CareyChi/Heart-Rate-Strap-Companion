package com.careychi.hrstrap.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import com.careychi.hrstrap.core.HeartRateAxis;
import java.text.SimpleDateFormat;
import java.util.*;

/** Reusable real-time/history chart with a one-hour viewport, real clock labels and edge rebound. */
public final class HeartRateChartView extends View {
    public record Point(long timestampMs, int bpm) {}

    private static final long WINDOW_MS = 60L * 60L * 1000L;
    private final Paint grid = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint axisText = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path linePath = new Path();
    private final Path fillPath = new Path();
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private List<Point> points = Collections.emptyList();
    private long viewStartMs = 0;
    private long dataStartMs = 0;
    private long dataEndMs = 0;
    private boolean followingLatest = true;
    private float downX;
    private long downStart;
    private ValueAnimator rebound;

    public HeartRateChartView(Context context) {
        super(context);
        setLayerType(LAYER_TYPE_SOFTWARE, null);
        grid.setColor(Color.argb(70, 125, 168, 151));
        grid.setStrokeWidth(Ui.dp(context, 1));
        axisText.setColor(Ui.MUTED);
        axisText.setTextSize(Ui.dp(context, 11));
        line.setColor(Ui.ACCENT);
        line.setStyle(Paint.Style.STROKE);
        line.setStrokeWidth(Ui.dp(context, 2.2f));
        line.setStrokeJoin(Paint.Join.ROUND);
        line.setStrokeCap(Paint.Cap.ROUND);
    }

    public void setPoints(List<Point> next) {
        points = next == null ? Collections.emptyList() : new ArrayList<>(next);
        if (!points.isEmpty()) {
            dataStartMs = points.get(0).timestampMs();
            dataEndMs = points.get(points.size() - 1).timestampMs();
            if (viewStartMs == 0 || followingLatest) viewStartMs = dataEndMs - WINDOW_MS;
        }
        invalidate();
    }

    public void append(Point p) {
        ArrayList<Point> next = new ArrayList<>(points);
        next.add(p);
        setPoints(next);
    }

    public void jumpToLatest() {
        followingLatest = true;
        if (dataEndMs > 0) viewStartMs = dataEndMs - WINDOW_MS;
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int left = Ui.dp(getContext(), 48);
        int top = Ui.dp(getContext(), 16);
        int right = getWidth() - Ui.dp(getContext(), 12);
        int bottom = getHeight() - Ui.dp(getContext(), 30);
        if (right <= left || bottom <= top) return;

        int max = 0;
        long sum = 0;
        int count = 0;
        for (Point p : points) {
            if (p.bpm() > 0) {
                max = Math.max(max, p.bpm());
                sum += p.bpm();
                count++;
            }
        }
        int avg = count == 0 ? 0 : (int) Math.round((double) sum / count);
        HeartRateAxis.Bands bands = HeartRateAxis.forChart(max, avg);
        int[] labels = {bands.maxBand(), bands.midBand(), bands.avgBand(), 0};

        for (int value : labels) {
            float y = yFor(value, bands.maxBand(), top, bottom);
            canvas.drawLine(left, y, right, y, grid);
            String text = Integer.toString(value);
            canvas.drawText(text, left - Ui.dp(getContext(), 8) - axisText.measureText(text), y + Ui.dp(getContext(), 4), axisText);
        }

        long end = viewStartMs + WINDOW_MS;
        for (int i = 0; i <= 4; i++) {
            long t = viewStartMs + WINDOW_MS * i / 4;
            float x = left + (right - left) * i / 4f;
            String label = timeFormat.format(new Date(t));
            float tw = axisText.measureText(label);
            canvas.drawText(label, Math.max(left, Math.min(right - tw, x - tw / 2f)), getHeight() - Ui.dp(getContext(), 8), axisText);
        }

        linePath.reset();
        fillPath.reset();
        boolean started = false;
        float firstX = 0, lastX = 0;
        for (Point p : points) {
            if (p.timestampMs() < viewStartMs || p.timestampMs() > end || p.bpm() <= 0) continue;
            float x = left + (right - left) * ((p.timestampMs() - viewStartMs) / (float) WINDOW_MS);
            float y = yFor(p.bpm(), bands.maxBand(), top, bottom);
            if (!started) {
                linePath.moveTo(x, y);
                fillPath.moveTo(x, bottom);
                fillPath.lineTo(x, y);
                firstX = x;
                started = true;
            } else {
                linePath.lineTo(x, y);
                fillPath.lineTo(x, y);
            }
            lastX = x;
        }
        if (started) {
            canvas.drawPath(linePath, line);
            fillPath.lineTo(lastX, bottom);
            fillPath.lineTo(firstX, bottom);
            fillPath.close();
            fill.setShader(new LinearGradient(0, top, 0, bottom,
                    Color.argb(105, 72, 240, 164), Color.TRANSPARENT, Shader.TileMode.CLAMP));
            canvas.drawPath(fillPath, fill);
            fill.setShader(null);
        }
    }

    private static float yFor(int bpm, int maxBand, int top, int bottom) {
        int clamped = Math.max(0, Math.min(maxBand, bpm));
        return bottom - (bottom - top) * (clamped / (float) maxBand);
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        if (dataEndMs <= 0) return true;
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN -> {
                if (rebound != null) rebound.cancel();
                downX = e.getX();
                downStart = viewStartMs;
                getParent().requestDisallowInterceptTouchEvent(true);
                return true;
            }
            case MotionEvent.ACTION_MOVE -> {
                float dx = e.getX() - downX;
                long delta = (long) (-dx / Math.max(1f, getWidth()) * WINDOW_MS);
                long proposed = downStart + delta;
                long min = minStart();
                long max = maxStart();
                if (proposed < min) proposed = min - (min - proposed) / 4;
                if (proposed > max) proposed = max + (proposed - max) / 4;
                long over = WINDOW_MS * 8 / 100;
                viewStartMs = Math.max(min - over, Math.min(max + over, proposed));
                followingLatest = Math.abs(viewStartMs - max) < 5000;
                invalidate();
                return true;
            }
            case MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                getParent().requestDisallowInterceptTouchEvent(false);
                animateIntoBounds();
                return true;
            }
        }
        return super.onTouchEvent(e);
    }

    private long minStart() {
        if (dataEndMs - dataStartMs <= WINDOW_MS) return dataEndMs - WINDOW_MS;
        return dataStartMs;
    }

    private long maxStart() { return dataEndMs - WINDOW_MS; }

    private void animateIntoBounds() {
        long min = minStart();
        long max = maxStart();
        long target = Math.max(min, Math.min(max, viewStartMs));
        if (target == viewStartMs) return;
        long start = viewStartMs;
        rebound = ValueAnimator.ofFloat(0f, 1f);
        rebound.setDuration(220);
        rebound.setInterpolator(new DecelerateInterpolator());
        rebound.addUpdateListener(a -> {
            float f = (float) a.getAnimatedValue();
            viewStartMs = start + (long) ((target - start) * f);
            invalidate();
        });
        rebound.start();
    }
}
