package com.tanle.t_shorten_url.util;

import com.tanle.t_shorten_url.exception.InvalidRangeException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalyticsRangeTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 10);

    private static AnalyticsRange range(Integer period, LocalDate from, LocalDate to) {
        return AnalyticsRange.of(period, from, to, "UTC", TODAY);
    }

    @Test
    void presetEndsToday() {
        AnalyticsRange r = range(7, null, null);
        assertEquals(LocalDate.of(2026, 10, 4), r.from());
        assertEquals(TODAY, r.to());
        assertEquals(7, r.bucketKeys().size());
    }

    @Test
    void defaultsToThirtyDays() {
        assertEquals(30, range(null, null, null).length());
    }

    @Test
    void previousRangeIsAdjacentAndSameLength() {
        AnalyticsRange p = range(7, null, null).previous();
        assertEquals(LocalDate.of(2026, 9, 27), p.from());
        assertEquals(LocalDate.of(2026, 10, 3), p.to());
    }

    @Test
    void longRangeBucketsByMondayAndClipsEdges() {
        AnalyticsRange r = range(null, LocalDate.of(2026, 1, 7), TODAY);
        assertTrue(r.weekly());
        assertEquals("2026-01-05", r.bucketKeys().get(0));
        assertEquals(r.bucketKeys().size(), r.bucketKeys().stream().distinct().count());
    }

    @Test
    void customRangeIsClampedToToday() {
        assertEquals(TODAY, range(null, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 12, 1)).to());
    }

    @Test
    void invalidInputIsRejected() {
        assertThrows(InvalidRangeException.class, () -> range(5, null, null));
        assertThrows(InvalidRangeException.class, () -> range(null, TODAY, TODAY.minusDays(1)));
        assertThrows(InvalidRangeException.class, () -> range(null, TODAY.plusDays(1), TODAY.plusDays(2)));
        assertThrows(InvalidRangeException.class, () -> range(null, TODAY.minusDays(400), TODAY));
        assertThrows(InvalidRangeException.class, () -> range(null, TODAY, null));
        assertThrows(InvalidRangeException.class, () -> AnalyticsRange.of(7, null, null, "Mars/Base", TODAY));
        assertThrows(InvalidRangeException.class, () -> AnalyticsRange.of(7, null, null, "GMT+7", TODAY));
    }

    @Test
    void instantsFollowTheTimeZone() {
        AnalyticsRange r = AnalyticsRange.of(7, null, null, "Asia/Ho_Chi_Minh", TODAY);
        assertEquals(Instant.parse("2026-10-03T17:00:00Z"), r.startInstant());
    }
}
