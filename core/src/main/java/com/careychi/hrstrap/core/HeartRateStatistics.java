package com.careychi.hrstrap.core;

import java.util.List;

public final class HeartRateStatistics {
    private HeartRateStatistics() {}

    public record Result(int maxBpm, int avgBpm, int sampleCount) {}

    public static Result of(List<Integer> values) {
        if (values == null || values.isEmpty()) return new Result(0, 0, 0);
        long sum = 0;
        int max = 0;
        int count = 0;
        for (Integer value : values) {
            if (value == null || value <= 0) continue;
            max = Math.max(max, value);
            sum += value;
            count++;
        }
        if (count == 0) return new Result(0, 0, 0);
        return new Result(max, (int) Math.round((double) sum / count), count);
    }
}
