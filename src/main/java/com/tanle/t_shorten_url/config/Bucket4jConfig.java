package com.tanle.t_shorten_url.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.Bucket4jLettuce;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.support.collections.RedisProperties;

import java.time.Duration;

@Configuration
public class Bucket4jConfig {
    public static final int CAPACITY = 2;
    public static final Duration REFILL_PERIOD = Duration.ofMinutes(1);

    @Bean
    public Bandwidth redirectUrlLimit() {
        return Bandwidth.builder()
                .capacity(CAPACITY)
                .refillIntervally(
                        CAPACITY,
                        REFILL_PERIOD
                )
                .build();
    }

    @Bean
    public ProxyManager<String> proxyManager(LettuceConnectionFactory connectionFactory) {
        RedisClient redisClient =
                (RedisClient) connectionFactory.getNativeClient();

        StatefulRedisConnection<String, byte[]> connection =
                redisClient.connect(
                        RedisCodec.of(
                                StringCodec.UTF8,
                                ByteArrayCodec.INSTANCE
                        )
                );

        return Bucket4jLettuce
                .casBasedBuilder(connection)
                .build();
    }
}