package com.careychi.hrstrap.core;

/** Parses Bluetooth SIG Heart Rate Measurement (0x2A37) characteristic values. */
public final class HeartRateMeasurementParser {
    private HeartRateMeasurementParser() {}

    public static int parseBpm(byte[] value) {
        if (value == null || value.length < 2) {
            throw new IllegalArgumentException("Heart Rate Measurement is too short");
        }
        int flags = value[0] & 0xFF;
        boolean isUInt16 = (flags & 0x01) != 0;
        if (!isUInt16) {
            return value[1] & 0xFF;
        }
        if (value.length < 3) {
            throw new IllegalArgumentException("16-bit Heart Rate Measurement is too short");
        }
        return (value[1] & 0xFF) | ((value[2] & 0xFF) << 8);
    }
}
