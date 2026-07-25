package com.tanle.t_shorten_url.cache.impl;

import com.tanle.t_shorten_url.cache.ShortUrlCacheService;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShortUrlCacheServiceImpl implements ShortUrlCacheService {
    private final StringRedisTemplate redisTemplate;
    private final String PREFIX = "url";

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
        redisTemplate.opsForValue()
                .increment(String.format("click:%s", code));
    }

    private String formatKey(String code) {
        return String.format("%s:%s", PREFIX, code);
    }
}
