package com.conorsheppard;

import com.conorsheppard.cache.InMemoryUrlCache;
import com.conorsheppard.cache.RedisUrlCache;
import com.conorsheppard.cache.UrlCache;
import com.conorsheppard.crawler.SimpleWebCrawler;
import com.conorsheppard.queue.ConcurrentQueue;
import com.conorsheppard.queue.KafkaQueue;
import com.conorsheppard.queue.UrlQueue;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Application {
    public static void main(String[] args) {

        if (args.length < 1) {
            log.info("Usage: Application <start-url> [queue-type] [cache-type] [max-threads]");
            return;
        }

        String startUrl = args[0];
        String queueType = args.length >= 2 ? args[1] : "concurrentQueue"; // Default: ConcurrentQueue
        String cacheType = args.length >= 3 ? args[2] : "inMemory"; // Default: InMemory cache
        int maxThreads = args.length >= 4 ? Integer.parseInt(args[3]) : 30; // Default: 30 threads

        // Select URL queue implementation
        UrlQueue queue = "kafka".equalsIgnoreCase(queueType)
                ? new KafkaQueue("localhost:9092")
                : new ConcurrentQueue();

        // Select URL cache implementation
        UrlCache cache = "redis".equalsIgnoreCase(cacheType)
                ? new RedisUrlCache("redis://localhost:6379")
                : new InMemoryUrlCache();

        log.info("Starting crawler with:");
        log.info("Start URL: {}", startUrl);
        log.info("Queue Type: {}", queueType);
        log.info("Cache Type: {}", cacheType);
        log.info("Max Threads: {}", maxThreads);

        SimpleWebCrawler crawler = new SimpleWebCrawler(startUrl, queue, cache, maxThreads);
        crawler.crawl();
    }
}
