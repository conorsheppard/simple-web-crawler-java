package com.conorsheppard.queue;

public interface UrlQueue {
    void enqueue(String url);
    String dequeue();
    boolean isEmpty();
    int size();
    boolean contains(String url);
}
