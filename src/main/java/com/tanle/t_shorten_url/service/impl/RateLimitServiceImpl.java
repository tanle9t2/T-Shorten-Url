package com.tanle.t_shorten_url.service.impl;

import com.tanle.t_shorten_url.service.RateLimitService;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitServiceImpl implements RateLimitService {
    private final ProxyManager<String> proxyManager;
    private final Bandwidth redirectUrlLimit;

    @Override
    public boolean tryConsume(String clientId) {
        Bucket bucket = proxyManager.builder()
                .build(clientId, () -> BucketConfiguration.builder()
                        .addLimit(redirectUrlLimit)
                        .build());

        return bucket.tryConsume(1);
    }
}
