package com.conorsheppard.cache;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryUrlCache implements UrlCache {
    private final Set<String> cache = ConcurrentHashMap.newKeySet();

    @Override
    public boolean contains(String url) {
        return cache.contains(url);
    }

    @Override
    public boolean add(String url) {
        return cache.add(url);
    }

    @Override
    public int size() {
        return cache.size();
    }
}
