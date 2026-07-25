package com.tanle.t_shorten_url.service;

import com.tanle.t_shorten_url.entity.ShortAnalyzed;
import com.tanle.t_shorten_url.event.ClickEvent;

import java.util.List;

public interface ShortAnalyzedService {
    List<ShortAnalyzed> getAnalyticsByShortUrlId(String shortUrlId);

    ShortAnalyzed save(ShortAnalyzed shortAnalyzed);

    void analyzeShortUrl(ClickEvent clickEvent);
}
