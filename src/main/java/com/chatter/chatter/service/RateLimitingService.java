package com.chatter.chatter.service;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
@Profile("!test")
public class RateLimitingService {

    private final ProxyManager<String> proxyManager;
    private final Supplier<BucketConfiguration>  bucketConfigurationSupplier;

    public Bucket resolveBucket(String email, String ip) {
        String key = email == null ? ip : email;
        return proxyManager.builder().build(key, bucketConfigurationSupplier);
    }

}
