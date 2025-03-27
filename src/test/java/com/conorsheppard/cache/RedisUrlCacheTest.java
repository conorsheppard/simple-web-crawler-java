package com.conorsheppard.cache;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RedisUrlCacheTest {
    private StatefulRedisConnection<String, String> mockRedisConnection;
    private RedisCommands<String, String> mockRedis;
    private RedisUrlCache redisUrlCache;

    @BeforeEach
    void setUp() {
        mockRedisConnection = mock(StatefulRedisConnection.class);
        mockRedis = mock(RedisCommands.class);
        when(mockRedisConnection.sync()).thenReturn(mockRedis);
        redisUrlCache = Mockito.spy(new RedisUrlCache(mockRedisConnection));
        doReturn(mockRedis).when(redisUrlCache).getRedis();
    }

    @Test
    void testContains_WhenUrlIsPresent() {
        String url = "http://example.com";
        when(mockRedis.sismember("web-crawler-url-cache", url)).thenReturn(true);

        assertTrue(redisUrlCache.contains(url));
        verify(mockRedis, times(1)).sismember("web-crawler-url-cache", url);
    }

    @Test
    void testContains_WhenUrlIsAbsent() {
        String url = "http://example.com";
        when(mockRedis.sismember("web-crawler-url-cache", url)).thenReturn(false);

        assertFalse(redisUrlCache.contains(url));
        verify(mockRedis, times(1)).sismember("web-crawler-url-cache", url);
    }

    @Test
    void testAdd_WhenUrlIsNew() {
        String url = "http://example.com";
        when(mockRedis.sadd("web-crawler-url-cache", url)).thenReturn(1L);

        assertTrue(redisUrlCache.add(url));
        verify(mockRedis, times(1)).sadd("web-crawler-url-cache", url);
    }

    @Test
    void testAdd_WhenUrlAlreadyExists() {
        String url = "http://example.com";
        when(mockRedis.sadd("web-crawler-url-cache", url)).thenReturn(0L);

        assertFalse(redisUrlCache.add(url));
        verify(mockRedis, times(1)).sadd("web-crawler-url-cache", url);
    }

    @Test
    void testSize() {
        when(mockRedis.scard("web-crawler-url-cache")).thenReturn(5L);

        assertEquals(5, redisUrlCache.size());
        verify(mockRedis, times(1)).scard("web-crawler-url-cache");
    }
}
