package com.chatter.chatter.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    public void evictCache(String pattern) {
        ScanOptions options = ScanOptions.scanOptions()
                .match(pattern)
                .count(100)
                .build();
        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            List<String> keys = new ArrayList<>();
            while (cursor.hasNext()) {
                keys.add(cursor.next());
                if (keys.size() >= 100) {
                    redisTemplate.unlink(keys);
                    keys.clear();
                }
            }
            if (!keys.isEmpty()) {
                redisTemplate.unlink(keys);
            }
        }
    }

    public void evictCacheSynchronized(String pattern) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    evictCache(pattern);
                }
            });
        }
        else {
            evictCache(pattern);
        }
    }

}
