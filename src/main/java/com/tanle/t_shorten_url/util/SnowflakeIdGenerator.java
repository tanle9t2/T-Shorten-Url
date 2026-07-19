package com.tanle.t_shorten_url.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Snowflake ID Generator
 * Generates 64-bit unique IDs based on Twitter's Snowflake algorithm.
 * <p>
 * Structure:
 * - 1 bit sign (always 0)
 * - 41 bits timestamp (custom epoch)
 * - 10 bits machine ID (configurable)
 * - 12 bits sequence number (auto-increments per millisecond)
 */
public class SnowflakeIdGenerator {

    // Custom Epoch (e.g., 2024-01-01T00:00:00Z)
    // You can adjust this, but once set, DO NOT CHANGE it in production.
    private static final long CUSTOM_EPOCH = 1704067200000L;

    // Bit lengths for each component
    private static final long MACHINE_ID_BITS = 10L;
    private static final long SEQUENCE_BITS = 12L;

    // Maximum values
    private static final long MAX_MACHINE_ID = ~(-1L << MACHINE_ID_BITS);
    private static final long MAX_SEQUENCE = ~(-1L << SEQUENCE_BITS);
    // Bit shifts
    private static final long MACHINE_ID_SHIFT = SEQUENCE_BITS;
    private static final long TIMESTAMP_LEFT_SHIFT = SEQUENCE_BITS + MACHINE_ID_BITS;
    private static long machineId;
    private static long sequence = 0L;
    private static long lastTimestamp = -1L;

    public SnowflakeIdGenerator(@Value("${worker.id:-1}") long workerId) {
        if (workerId > MAX_MACHINE_ID || workerId < 0) {
            throw new IllegalArgumentException(String.format("Machine Id can't be greater than %d or less than 0", MAX_MACHINE_ID));
        }
        this.machineId = workerId;
    }

    /**
     * Generates a new unique ID.
     *
     * @return 64-bit unique integer ID
     */
    public static synchronized long nextId() {
        long currentTimestamp = timestamp();

        if (currentTimestamp < lastTimestamp) {
            throw new IllegalStateException("Clock moved backwards. Refusing to generate id");
        }

        if (currentTimestamp == lastTimestamp) {
            // Same millisecond, increment sequence
            sequence = (sequence + 1) & MAX_SEQUENCE;
            // If sequence overflows, wait till next millisecond
            if (sequence == 0) {
                currentTimestamp = waitNextMillis(currentTimestamp);
            }
        } else {
            // New millisecond, reset sequence
            sequence = 0L;
        }

        lastTimestamp = currentTimestamp;

        // Shift and combine bits to form the 64-bit ID
        return ((currentTimestamp - CUSTOM_EPOCH) << TIMESTAMP_LEFT_SHIFT)
                | (machineId << MACHINE_ID_SHIFT)
                | sequence;
    }

    private static long waitNextMillis(long currentTimestamp) {
        while (currentTimestamp <= lastTimestamp) {
            currentTimestamp = timestamp();
        }
        return currentTimestamp;
    }

    private static long timestamp() {
        return System.currentTimeMillis();
    }
}
