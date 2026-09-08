package com.careychi.hrstrap.core;

/** Axis rules shared by the full charts and the compact overlay trend. */
public final class HeartRateAxis {
    private HeartRateAxis() {}

    public record Bands(int maxBand, int midBand, int avgBand, int zero) {}
    /** Legacy overlay bands kept for the service API; the V0.0.2B mini trend owns its visible scale. */
    public record OverlayBands(int maxBand, int avgBand) {}
    /** Three-value scale derived from the maximum BPM visible in the latest 60-second mini trend. */
    public record OverlayScale(int maxBand, int midBand, int minBand) {}

    public static int ceil25(int value) {
        if (value <= 0) return 0;
        return ((value + 24) / 25) * 25;
    }

    public static int floor25(int value) {
        if (value <= 0) return 0;
        return (value / 25) * 25;
    }

    public static int ceil50(int value) {
        if (value <= 0) return 0;
        return ((value + 49) / 50) * 50;
    }

    public static OverlayBands forOverlay(int maxBpm, int avgBpm) {
        int avgBand = floor25(avgBpm);
        int maxBand = Math.max(25, ceil25(maxBpm));
        maxBand = Math.max(maxBand, avgBand + 25);
        return new OverlayBands(maxBand, avgBand);
    }

    /**
     * V0.0.2B overlay scale:
     * - maximum: 50-bpm steps, minimum ceiling 100;
     * - middle: maximum - 50;
     * - minimum: 20% of maximum.
     */
    public static OverlayScale forOverlayRecent(int recentMaxBpm) {
        int maxBand = Math.max(100, ceil50(recentMaxBpm));
        int midBand = maxBand - 50;
        int minBand = maxBand / 5;
        return new OverlayScale(maxBand, midBand, minBand);
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
