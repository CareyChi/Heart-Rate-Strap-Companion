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

    @Test void overlayRecentScaleUsesFiftyBpmStepsAndDynamicMinimum() {
        assertOverlayScale(0, 100, 50, 20);
        assertOverlayScale(49, 100, 50, 20);
        assertOverlayScale(79, 100, 50, 20);
        assertOverlayScale(100, 100, 50, 20);
        assertOverlayScale(101, 150, 100, 30);
        assertOverlayScale(125, 150, 100, 30);
        assertOverlayScale(150, 150, 100, 30);
        assertOverlayScale(151, 200, 150, 40);
        assertOverlayScale(168, 200, 150, 40);
        assertOverlayScale(200, 200, 150, 40);
        assertOverlayScale(201, 250, 200, 50);
    }

    private static void assertOverlayScale(int recentMax, int expectedMax, int expectedMid, int expectedMin) {
        HeartRateAxis.OverlayScale scale = HeartRateAxis.forOverlayRecent(recentMax);
        assertEquals(expectedMax, scale.maxBand());
        assertEquals(expectedMid, scale.midBand());
        assertEquals(expectedMin, scale.minBand());
    }
}
