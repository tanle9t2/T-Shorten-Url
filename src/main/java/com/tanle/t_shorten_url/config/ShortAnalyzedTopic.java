package com.tanle.t_shorten_url.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class ShortAnalyzedTopic {
    @Value("${app.kafka.short-analyzed-topic}")
    private String topic;

    @Bean
    public NewTopic orderTopic() {
        return TopicBuilder
                .name(topic)
                .replicas(1)
                .partitions(1)
                .build();
    }
}
