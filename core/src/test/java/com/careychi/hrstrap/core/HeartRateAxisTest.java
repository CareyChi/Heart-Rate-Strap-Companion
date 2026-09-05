package com.careychi.hrstrap.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HeartRateAxisTest {
    @Test void ceil25CoversBoundaries() {
        assertEquals(0, HeartRateAxis.ceil25(0));
        assertEquals(25, HeartRateAxis.ceil25(1));
        assertEquals(25, HeartRateAxis.ceil25(24));
        assertEquals(25, HeartRateAxis.ceil25(25));
        assertEquals(50, HeartRateAxis.ceil25(26));
        assertEquals(100, HeartRateAxis.ceil25(100));
        assertEquals(125, HeartRateAxis.ceil25(101));
        assertEquals(125, HeartRateAxis.ceil25(125));
        assertEquals(150, HeartRateAxis.ceil25(126));
    }

    @Test void chartAlwaysHasIndependentMiddleBand() {
        HeartRateAxis.Bands bands = HeartRateAxis.forChart(124, 118);
        assertEquals(175, bands.maxBand());
        assertEquals(150, bands.midBand());
        assertEquals(125, bands.avgBand());
        assertEquals(0, bands.zero());
    }
}
