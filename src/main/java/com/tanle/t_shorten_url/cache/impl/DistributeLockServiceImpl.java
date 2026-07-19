package com.tanle.t_shorten_url.cache.impl;

import com.tanle.t_shorten_url.cache.DistributeLockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DistributeLockServiceImpl implements DistributeLockService {
    private final StringRedisTemplate redisTemplate;

    @Override
    public Boolean acquireLock(String key) {
        Boolean lock = redisTemplate.opsForValue()
                .setIfAbsent(
                        String.format("lock:%s", key),
                        UUID.randomUUID().toString(),
                        Duration.ofSeconds(5)
                );

        return lock;
    }
    @Override
    public void releaseLock(String key) {
        redisTemplate.delete(String.format("lock:%s", key));
    }
}
