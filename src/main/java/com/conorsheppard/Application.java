package com.conorsheppard;

import com.conorsheppard.crawler.SimpleWebCrawler;
import com.conorsheppard.queue.ConcurrentQueue;
import com.conorsheppard.queue.KafkaQueue;
import com.conorsheppard.queue.UrlQueue;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

@Slf4j
public class Application {
    public static void main(String[] args) {
        Arrays.stream(args).forEach(arg -> log.info("arg: {}", arg));
        if (args.length < 2) {
            log.info("Usage: Application <start-url> <queue-type> [max-threads]");
            return;
        }
        String startUrl = args[0];
        String queueType = args[1];
        int maxThreads = args.length >= 3 ? Integer.parseInt(args[2]) : 30;

        UrlQueue queue;
        if ("kafka".equalsIgnoreCase(queueType)) {
            queue = new KafkaQueue("localhost:9092");
        } else {
            queue = new ConcurrentQueue();
        }

        SimpleWebCrawler crawler = new SimpleWebCrawler(startUrl, queue, maxThreads);
        crawler.crawl();
    }
}
