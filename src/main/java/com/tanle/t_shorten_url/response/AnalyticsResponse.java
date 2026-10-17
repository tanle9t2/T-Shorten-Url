package com.tanle.t_shorten_url.response;

import java.util.List;

public record AnalyticsResponse(
        Summary summary,
        List<ActivityPoint> activitySeries,
        List<TopLink> topLinks) {

    public record Summary(Metrics current, Metrics previous) {
    }

    public record Metrics(long linksCreated, long totalClicks, long linksWithClicks, double averageClicks) {
    }

    // Key is a day (YYYY-MM-DD), or the Monday of the week when the range is bucketed weekly.
    public record ActivityPoint(String key, long linksCreated, long clicks) {
    }

    public record TopLink(String id, String shortCode, String originalUrl, long clicks, double share) {
    }
}
