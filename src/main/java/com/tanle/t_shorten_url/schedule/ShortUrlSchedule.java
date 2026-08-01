package com.tanle.t_shorten_url.schedule;

import com.tanle.t_shorten_url.cache.ShortUrlCacheService;
import com.tanle.t_shorten_url.entity.ShortUrl;
import com.tanle.t_shorten_url.repository.ShortUrlRepository;
import com.tanle.t_shorten_url.service.impl.ShortUrlServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class ShortUrlSchedule {

    private final ShortUrlServiceImpl shortUrlService;
    private final ShortUrlRepository shortUrlRepository;
    private final ShortUrlCacheService cacheService;

    @Scheduled(fixedRate = 60_000)
    public void flushViewCounterToMongo() {
        Map<Object, Object> views = this.cacheService.getViewToFlush();
        if (views == null || views.isEmpty()) {
            return;
        }

        log.info("Start flush views...");
        Set<String> shortCodes = views.keySet().stream()
                .map(String::valueOf)
                .collect(Collectors.toSet());

        List<ShortUrl> shortUrls = this.shortUrlRepository.findByShortCodeIn(shortCodes);
        for (ShortUrl shortUrl : shortUrls) {
            Object viewValue = views.get(shortUrl.getShortCode());
            if (viewValue != null) {
                long viewsCount = Long.parseLong(viewValue.toString());
                shortUrl.setViews(viewsCount);
            }
        }
        this.shortUrlRepository.saveAll(shortUrls);
        this.cacheService.deleteFlushingView();
        log.info("Views synced successfully");
    }
}
