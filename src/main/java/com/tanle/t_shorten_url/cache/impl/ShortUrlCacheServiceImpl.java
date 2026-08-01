package com.tanle.t_shorten_url.cache.impl;

import com.tanle.t_shorten_url.cache.ShortUrlCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShortUrlCacheServiceImpl implements ShortUrlCacheService {
    private final StringRedisTemplate redisTemplate;
    private final String PREFIX = "url";
    private final String VIEWS_KEY = "analytics:view";
    private final String VIEWS_KEY_FLUSHING = "analytics:view:flushing";

    @Override
    public Optional<String> getShortUrl(String code) {
        String cacheShortUrl = redisTemplate.opsForValue()
                .get(formatKey(code));

        return Optional.ofNullable(cacheShortUrl);
    }

    @Override
    public void cacheNotFound(String code) {
        redisTemplate.opsForValue()
                .set(formatKey(code),
                        "",
                        Duration.ofMinutes(5));
    }

    @Override
    public void createShortUrl(String code, String originalUrl) {
        redisTemplate.opsForValue()
                .set(formatKey(code),
                        originalUrl,
                        //Avoid Cache Avalanche.
                        // All key can expire at same time(Ex: 100.000 req, 100.000 key expired -> query DB at same time)
                        Duration.ofHours(24).plus(Duration.ofMinutes(new Random().nextInt(5))));
    }

    @Override
    public void invalidCache(String code) {
        redisTemplate.delete(formatKey(code));
    }

    @Override
    public void increaseTotalView(String code) {
        redisTemplate.opsForHash()
                .increment(VIEWS_KEY,
                        code, 1);
    }

    @Override
    public Long getView(String code) {
        Object value = redisTemplate.opsForHash()
                .get(VIEWS_KEY, code);

        if (value == null) return 0L;

        return Long.parseLong(value.toString());
    }

    @Override
    public Map<Object, Object> getViewToFlush() {
        Map<Object, Object> counters =
                redisTemplate.opsForHash()
                        .entries("analytics:view");
        //Avoid during flushing, new request come and counter was read
        //Rename to new request will automatically create new hash
        redisTemplate.rename(VIEWS_KEY, VIEWS_KEY_FLUSHING);
        return counters;
    }

    @Override
    public void deleteFlushingView() {
        redisTemplate.delete(VIEWS_KEY_FLUSHING);
    }

    private String formatKey(String code) {
        return String.format("%s:%s", PREFIX, code);
    }
}
