package com.conorsheppard.cache;

// Keeps track of all the URLs in the queue but, unlike the queue, offers quick lookup
public interface UrlCache {
    boolean contains(String url);
    boolean add(String url);
    int size();
}
