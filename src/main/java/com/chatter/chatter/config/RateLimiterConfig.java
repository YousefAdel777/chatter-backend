package com.chatter.chatter.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.Duration;
import java.util.function.Supplier;

@Configuration
@Profile("!test")
public class RateLimiterConfig {

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    @Value("${spring.data.redis.ssl.enabled}")
    private boolean redisSslEnabled;

    @Value("${app.rate.limit.capacity}")
    private int rateLimitCapacity;

    @Value("${app.rate.limit.refill-per-minute}")
    private int rateLimitRefillPerMinute;

    @Bean
    public RedisClient redisClient() {
        return RedisClient.create(RedisURI.builder()
                        .withHost(redisHost)
                        .withPort(redisPort)
                        .withSsl(redisSslEnabled)
                        .build());
    }

    @Bean
    public ProxyManager<String> lettuceBasedProxyManager(RedisClient redisClient) {
        StatefulRedisConnection<String,byte[]> redisConnection = redisClient.
                connect(RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE));
        return LettuceBasedProxyManager.builderFor(redisConnection)
                .build();
    }

    @Bean
    public Supplier<BucketConfiguration> bucketConfiguration() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(rateLimitCapacity)
                .refillGreedy(rateLimitRefillPerMinute, Duration.ofMinutes(1))
                .build();
        return () -> BucketConfiguration.builder()
                .addLimit(limit)
                .build();
    }
}