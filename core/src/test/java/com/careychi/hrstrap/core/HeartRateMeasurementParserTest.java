package com.careychi.hrstrap.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HeartRateMeasurementParserTest {
    @Test void parsesEightBit() {
        assertEquals(123, HeartRateMeasurementParser.parseBpm(new byte[]{0x00, 123}));
    }

    @Test void parsesSixteenBitLittleEndian() {
        assertEquals(300, HeartRateMeasurementParser.parseBpm(new byte[]{0x01, 0x2C, 0x01}));
    }

    @Test void rejectsShortPayloads() {
        assertThrows(IllegalArgumentException.class, () -> HeartRateMeasurementParser.parseBpm(new byte[]{0x00}));
        assertThrows(IllegalArgumentException.class, () -> HeartRateMeasurementParser.parseBpm(new byte[]{0x01, 0x2C}));
    }
}
