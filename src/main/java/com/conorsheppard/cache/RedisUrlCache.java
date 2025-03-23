package com.conorsheppard.cache;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;

public class RedisUrlCache implements UrlCache {
    private final RedisCommands<String, String> redis;
    private static final String VISITED_URLS = "web-crawler-url-cache";

    public RedisUrlCache(String redisUrl) {
        RedisClient client = RedisClient.create(redisUrl);
        StatefulRedisConnection<String, String> connection = client.connect();
        this.redis = connection.sync();
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
