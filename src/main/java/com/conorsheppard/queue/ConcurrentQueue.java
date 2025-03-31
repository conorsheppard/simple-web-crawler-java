package com.conorsheppard.queue;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ConcurrentQueue implements UrlQueue {
    private final Queue<String> queue = new ConcurrentLinkedQueue<>();

    @Override
    public void enqueue(String url) {
        queue.add(url);
    }

    @Override
    public String dequeue() {
        return queue.poll();
    }

    @Override
    public boolean isEmpty() {
        return queue.isEmpty();
    }

    @Override
    public int size() {
        return queue.size();
    }
}
