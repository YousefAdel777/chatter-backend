package com.chatter.chatter.unit.service;

import com.chatter.chatter.service.CacheService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CacheServiceTests {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private Cursor<String> cursor;

    @InjectMocks
    private CacheService cacheService;

    private MockedStatic<TransactionSynchronizationManager> syncManagerMock;

    @BeforeEach
    void setUp() {
        syncManagerMock = mockStatic(TransactionSynchronizationManager.class);
    }

    @AfterEach
    void tearDown() {
        syncManagerMock.close();
    }

    @Test
    void evictCache_Success() {
        String pattern = "test:*";
        when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(cursor);
        when(cursor.hasNext()).thenReturn(true, true, false);
        when(cursor.next()).thenReturn("key1", "key2");

        cacheService.evictCache(pattern);

        verify(redisTemplate, atLeastOnce()).scan(any(ScanOptions.class));
        verify(redisTemplate).unlink(anyList());
        verify(cursor).close();
    }

    @Test
    void evictCacheSynchronized_NoTransaction() {
        String pattern = "test:*";
        syncManagerMock.when(TransactionSynchronizationManager::isActualTransactionActive).thenReturn(false);
        when(redisTemplate.scan(any())).thenReturn(cursor);

        cacheService.evictCacheSynchronized(pattern);

        verify(redisTemplate).scan(any());
        syncManagerMock.verify(() -> TransactionSynchronizationManager.registerSynchronization(any()), never());
    }

    @Test
    void evictCacheSynchronized_WithTransaction() {
        String pattern = "test:*";
        syncManagerMock.when(TransactionSynchronizationManager::isActualTransactionActive).thenReturn(true);
        when(redisTemplate.scan(any())).thenReturn(cursor);

        cacheService.evictCacheSynchronized(pattern);

        verify(redisTemplate, never()).scan(any());

        ArgumentCaptor<TransactionSynchronization> captor = ArgumentCaptor.forClass(TransactionSynchronization.class);
        syncManagerMock.verify(() -> TransactionSynchronizationManager.registerSynchronization(captor.capture()));

        TransactionSynchronization sync = captor.getValue();
        sync.afterCommit();

        verify(redisTemplate).scan(any());
    }
}