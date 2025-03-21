package com.conorsheppard.cache;

public interface UrlCache {
    boolean contains(String url);
    boolean add(String url);
    int size();
}
