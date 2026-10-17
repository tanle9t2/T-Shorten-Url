package com.tanle.t_shorten_url.service;

import com.tanle.t_shorten_url.response.AnalyticsResponse;
import com.tanle.t_shorten_url.response.ShortCodeAnalyticsResponse;

import java.time.LocalDate;

public interface AnalyticsService {
    AnalyticsResponse getAnalytics(String userId, Integer period, LocalDate from, LocalDate to, String timeZone);

    ShortCodeAnalyticsResponse getShortCodeAnalytics(String userId, String shortCode, Integer period,
                                                     LocalDate from, LocalDate to, String timeZone);
}
