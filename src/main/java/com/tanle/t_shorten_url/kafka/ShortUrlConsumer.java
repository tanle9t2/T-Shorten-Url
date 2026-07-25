package com.tanle.t_shorten_url.kafka;

import com.tanle.t_shorten_url.cache.ShortUrlCacheService;
import com.tanle.t_shorten_url.event.ClickEvent;
import com.tanle.t_shorten_url.service.ShortAnalyzedService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.BackOff;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShortUrlConsumer {
    private final ShortAnalyzedService shortAnalyzedService;
    private final ShortUrlCacheService shortUrlCacheService;

    @RetryableTopic(
            attempts = "3",
            backOff = @BackOff(delay = 2000),
            dltTopicSuffix = ".DLT"
    )
    @KafkaListener(topics = "${app.kafka.short-analyzed-topic}", groupId = "analyzed-group")
    public void consumeEvent(List<ClickEvent> clickEvents) {
        for (ClickEvent event : clickEvents) {
            this.shortAnalyzedService.analyzeShortUrl(event);
            this.shortUrlCacheService.increaseTotalView(event.shortCode());

            log.info("Consume clickEvent {}", event.shortCode());
        }
    }
}
