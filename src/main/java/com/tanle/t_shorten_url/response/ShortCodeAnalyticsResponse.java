package com.tanle.t_shorten_url.response;

import java.util.List;

public record ShortCodeAnalyticsResponse(
        String shortCode,
        String originalUrl,
        Summary summary,
        List<ClickPoint> clickSeries,
        List<Share> countries,
        List<Share> os,
        List<Share> browsers,
        List<Share> referers) {

    public record Summary(Total current, Total previous) {
    }

    public record Total(long totalClicks) {
    }

    // Key is a day (YYYY-MM-DD), or the Monday of the week when the range is bucketed weekly.
    public record ClickPoint(String key, long clicks) {
    }

    public record Share(String name, long clicks, double share) {
    }
}
