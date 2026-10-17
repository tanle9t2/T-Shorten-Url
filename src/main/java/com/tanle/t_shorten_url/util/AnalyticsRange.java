package com.tanle.t_shorten_url.util;

import com.tanle.t_shorten_url.exception.InvalidRangeException;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

// Both ends are inclusive days in the viewer's time zone.
public record AnalyticsRange(LocalDate from, LocalDate to, ZoneId zone) {
    public static final int DEFAULT_PERIOD = 30;
    public static final int MAX_RANGE_DAYS = 365;
    // Above this length a daily chart is unreadable, so buckets become weeks.
    public static final int DAILY_LIMIT_DAYS = 90;
    private static final Set<Integer> PERIODS = Set.of(7, 30, 90);

    public static AnalyticsRange of(Integer period, LocalDate from, LocalDate to, String timeZone, LocalDate today) {
        ZoneId zone = parseZone(timeZone);
        LocalDate currentDay = today == null ? LocalDate.now(zone) : today;

        if (from != null || to != null) {
            return custom(from, to, zone, currentDay);
        }
        int days = period == null ? DEFAULT_PERIOD : period;
        if (!PERIODS.contains(days)) {
            throw new InvalidRangeException("period must be 7, 30 or 90");
        }
        return new AnalyticsRange(currentDay.minusDays(days - 1L), currentDay, zone);
    }

    private static AnalyticsRange custom(LocalDate from, LocalDate to, ZoneId zone, LocalDate today) {
        if (from == null || to == null) {
            throw new InvalidRangeException("from and to must be provided together");
        }
        if (from.isAfter(today)) {
            throw new InvalidRangeException("from cannot be in the future");
        }
        if (to.isBefore(from)) {
            throw new InvalidRangeException("to cannot be before from");
        }
        AnalyticsRange range = new AnalyticsRange(from, to.isAfter(today) ? today : to, zone);
        if (range.length() > MAX_RANGE_DAYS) {
            throw new InvalidRangeException("range cannot be longer than " + MAX_RANGE_DAYS + " days");
        }
        return range;
    }

    private static ZoneId parseZone(String timeZone) {
        if (timeZone == null || timeZone.isBlank()) {
            return ZoneId.of("UTC");
        }
        // Mongo only understands region ids, so fixed offsets like "GMT+7" would fail there with a 500.
        if (!ZoneId.getAvailableZoneIds().contains(timeZone)) {
            throw new InvalidRangeException("unknown time zone: " + timeZone);
        }
        return ZoneId.of(timeZone);
    }

    public long length() {
        return ChronoUnit.DAYS.between(from, to) + 1;
    }

    public boolean weekly() {
        return length() > DAILY_LIMIT_DAYS;
    }

    public AnalyticsRange previous() {
        LocalDate previousTo = from.minusDays(1);
        return new AnalyticsRange(previousTo.minusDays(length() - 1), previousTo, zone);
    }

    public String bucketKey(LocalDate day) {
        return (weekly() ? day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) : day).toString();
    }

    // Only days inside the range seed buckets, so edge weeks are clipped.
    public List<String> bucketKeys() {
        Set<String> keys = new LinkedHashSet<>();
        for (long offset = 0; offset < length(); offset++) {
            keys.add(bucketKey(from.plusDays(offset)));
        }
        return new ArrayList<>(keys);
    }

    public Instant startInstant() {
        return from.atStartOfDay(zone).toInstant();
    }

    public Instant endExclusiveInstant() {
        return to.plusDays(1).atStartOfDay(zone).toInstant();
    }
}
