package com.tanle.t_shorten_url.config;

import com.tanle.t_shorten_url.entity.ShortAnalyzed;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;

@Configuration
public class MongoIndexConfig {

    // Global auto-index-creation is off on purpose: it clashes with indexes that already exist
    // under other names (e.g. short_url.shortCode), so only the index analytics needs is created here.
    @Bean
    ApplicationRunner analyticsIndexes(MongoTemplate mongoTemplate) {
        return args -> mongoTemplate.indexOps(ShortAnalyzed.class).createIndex(
                new Index().on("shortCode", Sort.Direction.ASC)
                        .on("createdAt", Sort.Direction.ASC)
                        .named("shortCode_createdAt"));
    }
}
