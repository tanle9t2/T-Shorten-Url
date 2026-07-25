package com.tanle.t_shorten_url.kafka;

import com.tanle.t_shorten_url.event.ClickEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShortUrlProducer {
    private final KafkaTemplate<String, ClickEvent> kafkaTemplate;
    @Value("${app.kafka.short-analyzed-topic}")
    private String topic;

    public void publishMessage(ClickEvent clickEvent) {
        kafkaTemplate.send(topic, String.valueOf(clickEvent.hashCode()), clickEvent);
        log.info("Click event published: " + clickEvent);
    }
}
