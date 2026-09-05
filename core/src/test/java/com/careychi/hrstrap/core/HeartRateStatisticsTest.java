package com.careychi.hrstrap.core;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class HeartRateStatisticsTest {
    @Test void emptyInputIsZeroed() {
        assertEquals(new HeartRateStatistics.Result(0, 0, 0), HeartRateStatistics.of(List.of()));
    }

    @Test void singleSampleIsStable() {
        assertEquals(new HeartRateStatistics.Result(123, 123, 1), HeartRateStatistics.of(List.of(123)));
    }

    @Test void multipleSamplesUseRoundedAverageAndMax() {
        assertEquals(new HeartRateStatistics.Result(140, 120, 3), HeartRateStatistics.of(List.of(100, 120, 140)));
    }

    @Test void invalidSamplesAreIgnored() {
        assertEquals(new HeartRateStatistics.Result(100, 100, 1), HeartRateStatistics.of(java.util.Arrays.asList(null, 0, -1, 100)));
    }
}
