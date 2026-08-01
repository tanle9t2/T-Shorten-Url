package com.tanle.t_shorten_url.cache;

import java.util.Map;
import java.util.Optional;

public interface ShortUrlCacheService {
    Optional<String> getShortUrl(String code);

    void cacheNotFound(String code);

    void createShortUrl(String code, String originalUrl);

    void invalidCache(String code);

    void increaseTotalView(String code);

    Long getView(String code);

    Map<Object, Object> getViewToFlush();

    void deleteFlushingView();
}
