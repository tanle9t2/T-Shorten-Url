package com.tanle.t_shorten_url.service.impl;

import com.tanle.t_shorten_url.cache.DistributeLockService;
import com.tanle.t_shorten_url.cache.ShortUrlCacheService;
import com.tanle.t_shorten_url.entity.ShortUrl;
import com.tanle.t_shorten_url.exception.ResourceNotFoundExeption;
import com.tanle.t_shorten_url.mapper.ShortUrlMapper;
import com.tanle.t_shorten_url.repository.ShortUrlRepository;
import com.tanle.t_shorten_url.request.ShorUrlCreatedRequest;
import com.tanle.t_shorten_url.response.ShortUrlResponse;
import com.tanle.t_shorten_url.service.ShortUrlService;
import com.tanle.t_shorten_url.util.ShortCodeGenerator;
import com.tanle.t_shorten_url.util.SnowflakeIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShortUrlServiceImpl implements ShortUrlService {

    private final ShortUrlRepository shortUrlRepository;
    private final DistributeLockService distributeLockService;
    private final ShortUrlCacheService shortUrlCacheService;
    private final ShortUrlMapper shortUrlMapper;
    private final ConcurrentHashMap<String, Object> locks = new ConcurrentHashMap<>();
    private final String PREFIX_APP = "tan.le";

    @Override
    public ShortUrlResponse findByShortUrl(String code) {
        log.info("Finding ShortUrl by shortCode: {}", code);
        ShortUrl shortUrl = shortUrlRepository.findByShortCode(code)
                .orElseThrow(() -> new ResourceNotFoundExeption("Not found " + code));

        return shortUrlMapper.convertToResponse(shortUrl);
    }

    @Override
    public String findRedirectUrl(String code) throws InterruptedException {
        log.info("Finding redirect by shortCode: {}", code);
        Optional<String> cacheUrl = shortUrlCacheService.getShortUrl(code);
        if (cacheUrl.isPresent()) {
            String url = cacheUrl.get();
            if (url.isEmpty()) {
                throw new ResourceNotFoundExeption("Not found " + code);
            }
            log.info("Hit cache for shortCode: {}", code);
            return url;
        }

        log.info("Missing cache for shortCode: {}", code);

        //Avoid bottleneck. Ex: 10 request have different code, thought they must be wait if we use Object for lock
        //Instead of locking by code, it helps to decrease bottleneck
        //However this way can solve problem when we have multiple instances,
        //because each instance has individually JVM -> if requests same key that are distributed to different instance,
        // they still query db
//        Object lock = locks.computeIfAbsent(code, (newCode) -> new Object());

//        synchronized (lock) {
//            Optional<String> reChecked = shortUrlCacheService.getShortUrl(code);
//            if (reChecked.isPresent()) {
//                String url = cacheUrl.get();
//                if (url.isEmpty()) {
//                    throw new ResourceNotFoundExeption("Not found " + code);
//                }
//                return url;
//            }
//            shortUrl = shortUrlRepository.findByShortCode(code)
//                    .or(() -> {
//                        //Avoid cache Penetration. EX: Attacker spam many code not exit, -> query directly DB
//                        shortUrlCacheService.cacheNotFound(code);
//                        return null;
//                    })
//                    .orElseThrow(() -> new ResourceNotFoundExeption("Not found " + code));
//        }
        String key = "url:" + code;
        boolean lock = distributeLockService.acquireLock(key);
        //Avoid Cache Breakdown. EX: 1 hot key expired when all request access to key -> query DB
        if (lock) {
            try {
                ShortUrl shortUrl = shortUrlRepository.findByShortCode(code)
                        .or(() -> {
                            //Avoid cache Penetration. EX: Attacker spam many code not exit, -> query directly DB
                            shortUrlCacheService.cacheNotFound(code);
                            return null;
                        })
                        .orElseThrow(() -> new ResourceNotFoundExeption("Not found " + code));
                shortUrlCacheService.createShortUrl(shortUrl.getShortCode(), shortUrl.getOriginalUrl());
                return shortUrl.getOriginalUrl();
            } finally {
                distributeLockService.releaseLock(key);
            }
        }

        //Waiting Another instance build cache
        // Cons: URL exists, but cache building takes longer than expected.
        Thread.sleep(500);
        cacheUrl = shortUrlCacheService.getShortUrl(code);
        if (!cacheUrl.isPresent()) {
            throw new ResourceNotFoundExeption("Not found " + code);
        }

        String url = cacheUrl.get();
        if (url.isEmpty()) {
            throw new ResourceNotFoundExeption("Not found " + code);
        }
        return url;
    }

    private String formatShorUrl(String code) {
        return String.format("%s/%s", PREFIX_APP, code);
    }

    @Override
    public String save(ShorUrlCreatedRequest shortUrl) {
        log.info("Saving ShortUrl: {}", shortUrl.getOriginalUrl());

        Long id = SnowflakeIdGenerator.nextId();
        String code = ShortCodeGenerator.encode(id);
        ShortUrl urlEntity = ShortUrl.builder()
                .originalUrl(shortUrl.getOriginalUrl())
                .id(String.valueOf(id))
                .shortCode(code)
                .userId("1")
                .build();

        shortUrlRepository.save(urlEntity);

        return formatShorUrl(code);
    }


}
