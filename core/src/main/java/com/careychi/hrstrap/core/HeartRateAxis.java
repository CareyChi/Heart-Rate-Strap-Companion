package com.careychi.hrstrap.core;

/** 25-bpm banded axis rules shared by live and historical charts. */
public final class HeartRateAxis {
    private HeartRateAxis() {}

    public record Bands(int maxBand, int midBand, int avgBand, int zero) {}
    public record OverlayBands(int maxBand, int avgBand) {}

    public static int ceil25(int value) {
        if (value <= 0) return 0;
        return ((value + 24) / 25) * 25;
    }

    public static int floor25(int value) {
        if (value <= 0) return 0;
        return (value / 25) * 25;
    }

    public static OverlayBands forOverlay(int maxBpm, int avgBpm) {
        int avgBand = floor25(avgBpm);
        int maxBand = Math.max(25, ceil25(maxBpm));
        maxBand = Math.max(maxBand, avgBand + 25);
        return new OverlayBands(maxBand, avgBand);
    }

    public static Bands forChart(int maxBpm, int avgBpm) {
        int avgBand = Math.max(25, ceil25(avgBpm));
        int rawMaxBand = Math.max(25, ceil25(maxBpm));
        int maxBand = Math.max(rawMaxBand, avgBand + 50);
        int target = (avgBand + maxBand) / 2;
        int rounded = Math.max(avgBand + 25, Math.min(maxBand - 25, ceil25(target)));
        if (rounded >= maxBand) rounded = maxBand - 25;
        if (rounded <= avgBand) rounded = avgBand + 25;
        return new Bands(maxBand, rounded, avgBand, 0);
    }
}
