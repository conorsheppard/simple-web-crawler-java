package com.conorsheppard.cache;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import lombok.Data;

@Data
public class RedisUrlCache implements UrlCache {
    private final RedisCommands<String, String> redis;
    private static final String VISITED_URLS = "web-crawler-url-cache";

    public RedisUrlCache(StatefulRedisConnection<String, String> redisCommands) {
        this.redis = redisCommands.sync();
    }

    @Override
    public boolean contains(String url) {
        return redis.sismember(VISITED_URLS, url);
    }

    @Override
    public boolean add(String url) {
        return redis.sadd(VISITED_URLS, url) > 0;
    }

    @Override
    public int size() {
        return redis.scard(VISITED_URLS).intValue();
    }
}
