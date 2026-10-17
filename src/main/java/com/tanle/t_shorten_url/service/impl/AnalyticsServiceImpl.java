package com.tanle.t_shorten_url.service.impl;

import com.tanle.t_shorten_url.entity.ShortUrl;
import com.tanle.t_shorten_url.repository.AnalyticsRepository;
import com.tanle.t_shorten_url.repository.AnalyticsRepository.ClickCount;
import com.tanle.t_shorten_url.repository.AnalyticsRepository.ValueCount;
import com.tanle.t_shorten_url.util.AppConstant;
import com.tanle.t_shorten_url.repository.ShortUrlRepository;
import com.tanle.t_shorten_url.response.AnalyticsResponse;
import com.tanle.t_shorten_url.response.AnalyticsResponse.ActivityPoint;
import com.tanle.t_shorten_url.response.AnalyticsResponse.Metrics;
import com.tanle.t_shorten_url.response.AnalyticsResponse.Summary;
import com.tanle.t_shorten_url.response.AnalyticsResponse.TopLink;
import com.tanle.t_shorten_url.response.ShortCodeAnalyticsResponse;
import com.tanle.t_shorten_url.response.ShortCodeAnalyticsResponse.ClickPoint;
import com.tanle.t_shorten_url.response.ShortCodeAnalyticsResponse.Share;
import com.tanle.t_shorten_url.exception.ResourceNotFoundException;
import com.tanle.t_shorten_url.service.AnalyticsService;
import com.tanle.t_shorten_url.util.AnalyticsRange;
import com.tanle.t_shorten_url.util.UserAgentClassifier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {
    private static final int TOP_LINKS_LIMIT = 5;
    private static final int BREAKDOWN_LIMIT = 5;
    private static final String OTHER = "Other";

    private final ShortUrlRepository shortUrlRepository;
    private final AnalyticsRepository analyticsRepository;

    @Override
    public AnalyticsResponse getAnalytics(String userId, Integer period, LocalDate from, LocalDate to,
                                          String timeZone) {
        AnalyticsRange range = AnalyticsRange.of(period, from, to, timeZone, null);
        AnalyticsRange previous = range.previous();

        // shortcut: loads every active link of the owner, move to a paged/aggregated query when volumes grow.
        List<ShortUrl> links = shortUrlRepository.findAllByUserId(userId).stream()
                .filter(link -> link.getCreatedAt() != null).toList();
        List<ClickCount> clicks = links.isEmpty()
                ? List.of()
                : analyticsRepository.countClicksByDay(
                        links.stream().map(ShortUrl::getShortCode).toList(),
                        previous.startInstant(), range.endExclusiveInstant(), range.zone());

        List<ClickCount> currentClicks = clicksIn(clicks, range);
        return new AnalyticsResponse(
                new Summary(metrics(links, currentClicks, range), metrics(links, clicksIn(clicks, previous), previous)),
                activitySeries(links, currentClicks, range),
                topLinks(links, currentClicks));
    }

    @Override
    public ShortCodeAnalyticsResponse getShortCodeAnalytics(String userId, String shortCode, Integer period,
                                                            LocalDate from, LocalDate to, String timeZone) {
        AnalyticsRange range = AnalyticsRange.of(period, from, to, timeZone, null);
        AnalyticsRange previous = range.previous();

        // Foreign and missing links look the same so a code's existence is not leaked across owners.
        ShortUrl link = shortUrlRepository.findByShortCode(shortCode)
                .filter(l -> userId.equals(l.getUserId()))
                .orElseThrow(() -> new ResourceNotFoundException("Not found " + shortCode));

        List<ClickCount> days = analyticsRepository.countClicksByDay(
                List.of(shortCode), previous.startInstant(), range.endExclusiveInstant(), range.zone());
        List<ClickCount> currentDays = clicksIn(days, range);
        long currentTotal = currentDays.stream().mapToLong(ClickCount::clicks).sum();
        long previousTotal = clicksIn(days, previous).stream().mapToLong(ClickCount::clicks).sum();

        Map<String, Long> series = new LinkedHashMap<>();
        range.bucketKeys().forEach(key -> series.put(key, 0L));
        currentDays.forEach(c -> series.merge(range.bucketKey(LocalDate.parse(c.day())), c.clicks(), Long::sum));

        // Fetched once because both the OS and the browser breakdown derive from it.
        List<ValueCount> userAgents = countsOf(range, shortCode, "userAgent");

        return new ShortCodeAnalyticsResponse(
                link.getShortCode(), link.getOriginalUrl(),
                new ShortCodeAnalyticsResponse.Summary(
                        new ShortCodeAnalyticsResponse.Total(currentTotal),
                        new ShortCodeAnalyticsResponse.Total(previousTotal)),
                series.entrySet().stream().map(e -> new ClickPoint(e.getKey(), e.getValue())).toList(),
                breakdown(countsOf(range, shortCode, "country"), v -> blankTo(v, UserAgentClassifier.UNKNOWN), currentTotal),
                breakdown(userAgents, UserAgentClassifier::os, currentTotal),
                breakdown(userAgents, UserAgentClassifier::browser, currentTotal),
                breakdown(countsOf(range, shortCode, "referer"), AnalyticsServiceImpl::refererHost, currentTotal));
    }

    private List<ValueCount> countsOf(AnalyticsRange range, String shortCode, String field) {
        return analyticsRepository.countClicksBy(field, shortCode, range.startInstant(), range.endExclusiveInstant());
    }

    private List<Share> breakdown(List<ValueCount> counts, Function<String, String> normalize, long total) {
        // Normalizing after grouping merges raw values (e.g. many user agents) into one label.
        Map<String, Long> merged = new HashMap<>();
        counts.forEach(c -> merged.merge(normalize.apply(c.value()), c.clicks(), Long::sum));

        List<Map.Entry<String, Long>> sorted = merged.entrySet().stream()
                .sorted(Comparator.comparing((Map.Entry<String, Long> e) -> e.getValue()).reversed()
                        .thenComparing(Map.Entry::getKey))
                .toList();

        List<Share> shares = new ArrayList<>();
        sorted.stream().limit(BREAKDOWN_LIMIT).forEach(e -> shares.add(share(e.getKey(), e.getValue(), total)));
        long rest = sorted.stream().skip(BREAKDOWN_LIMIT).mapToLong(Map.Entry::getValue).sum();
        if (rest > 0) {
            // The classifier can already emit "Other" in the top rows; merge so the label stays unique.
            shares.stream().filter(x -> OTHER.equals(x.name())).findFirst().ifPresent(shares::remove);
            long existing = sorted.stream().limit(BREAKDOWN_LIMIT).filter(e -> OTHER.equals(e.getKey()))
                    .mapToLong(Map.Entry::getValue).sum();
            shares.add(share(OTHER, rest + existing, total));
        }
        return shares;
    }

    private static Share share(String name, long clicks, long total) {
        return new Share(name, clicks, total == 0 ? 0 : (double) clicks / total);
    }

    private static String blankTo(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    // NO_REFERER is the placeholder the publisher stores when the Referer header is absent.
    private static String refererHost(String referer) {
        if (referer == null || referer.isBlank() || AppConstant.NO_REFERER.equals(referer)) {
            return "direct";
        }
        try {
            String host = URI.create(referer.trim()).getHost();
            return host == null ? UserAgentClassifier.UNKNOWN : host.toLowerCase(Locale.ROOT);
        } catch (IllegalArgumentException e) {
            return UserAgentClassifier.UNKNOWN;
        }
    }

    private static List<ClickCount> clicksIn(List<ClickCount> clicks, AnalyticsRange range) {
        // ISO day keys compare correctly as strings.
        String from = range.from().toString();
        String to = range.to().toString();
        return clicks.stream().filter(c -> c.day().compareTo(from) >= 0 && c.day().compareTo(to) <= 0).toList();
    }

    private static LocalDate createdDay(ShortUrl link, AnalyticsRange range) {
        return link.getCreatedAt().atZone(range.zone()).toLocalDate();
    }

    private static boolean createdIn(ShortUrl link, AnalyticsRange range) {
        LocalDate day = createdDay(link, range);
        return !day.isBefore(range.from()) && !day.isAfter(range.to());
    }

    private static Metrics metrics(List<ShortUrl> links, List<ClickCount> clicks, AnalyticsRange range) {
        long linksCreated = links.stream().filter(link -> createdIn(link, range)).count();
        long totalClicks = clicks.stream().mapToLong(ClickCount::clicks).sum();
        long linksWithClicks = clicks.stream().map(ClickCount::shortCode).distinct().count();
        double averageClicks = linksWithClicks == 0 ? 0 : (double) totalClicks / linksWithClicks;
        return new Metrics(linksCreated, totalClicks, linksWithClicks, averageClicks);
    }

    private static List<ActivityPoint> activitySeries(List<ShortUrl> links, List<ClickCount> clicks,
                                                      AnalyticsRange range) {
        Map<String, long[]> buckets = new LinkedHashMap<>();
        range.bucketKeys().forEach(key -> buckets.put(key, new long[2]));

        links.stream().filter(link -> createdIn(link, range))
                .forEach(link -> buckets.get(range.bucketKey(createdDay(link, range)))[0]++);
        clicks.forEach(click -> buckets.get(range.bucketKey(LocalDate.parse(click.day())))[1] += click.clicks());

        return buckets.entrySet().stream()
                .map(e -> new ActivityPoint(e.getKey(), e.getValue()[0], e.getValue()[1]))
                .toList();
    }

    private static List<TopLink> topLinks(List<ShortUrl> links, List<ClickCount> clicks) {
        long totalClicks = clicks.stream().mapToLong(ClickCount::clicks).sum();
        Map<String, Long> clicksByCode = clicks.stream()
                .collect(Collectors.groupingBy(ClickCount::shortCode, Collectors.summingLong(ClickCount::clicks)));
        Map<String, ShortUrl> linksByCode = links.stream()
                .collect(Collectors.toMap(ShortUrl::getShortCode, Function.identity()));

        return clicksByCode.entrySet().stream()
                .filter(e -> linksByCode.containsKey(e.getKey()))
                .sorted(Comparator
                        .comparing((Map.Entry<String, Long> e) -> e.getValue()).reversed()
                        .thenComparing(e -> linksByCode.get(e.getKey()).getCreatedAt(), Comparator.reverseOrder()))
                .limit(TOP_LINKS_LIMIT)
                .map(e -> {
                    ShortUrl link = linksByCode.get(e.getKey());
                    return new TopLink(link.getId(), link.getShortCode(), link.getOriginalUrl(), e.getValue(),
                            (double) e.getValue() / totalClicks);
                })
                .toList();
    }
}
