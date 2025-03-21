package com.conorsheppard;

import com.conorsheppard.cache.InMemoryUrlCache;
import com.conorsheppard.cache.RedisUrlCache;
import com.conorsheppard.cache.UrlCache;
import com.conorsheppard.crawler.SimpleWebCrawler;
import com.conorsheppard.queue.ConcurrentQueue;
import com.conorsheppard.queue.KafkaQueue;
import com.conorsheppard.queue.UrlQueue;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

@Slf4j
@Command(name = "WebCrawler", mixinStandardHelpOptions = true, version = "1.0",
        description = "A simple web crawler with configurable queue and cache options.")
public class Application implements Callable<Integer> {

    @Option(names = {"-u", "--url"}, description = "Start URL", required = true)
    private String startUrl;

    @Option(names = {"-q", "--queue"}, description = "Queue type (concurrentQueue, kafka)", defaultValue = "concurrentQueue")
    private String queueType;

    @Option(names = {"-c", "--cache"}, description = "Cache type (inMemory, redis)", defaultValue = "inMemory")
    private String cacheType;

    @Option(names = {"-t", "--threads"}, description = "Max number of threads", defaultValue = "30")
    private int maxThreads;

    @Override
    public Integer call() {
        log.info("Starting Web Crawler with URL: {}", startUrl);
        log.info("Queue Type: {}, Cache Type: {}, Max Threads: {}", queueType, cacheType, maxThreads);

        // Initialize queue]
        UrlQueue queue = "kafka".equalsIgnoreCase(queueType)
                ? new KafkaQueue("localhost:9092")
                : new ConcurrentQueue();

        // Initialize cache
        UrlCache cache = "redis".equalsIgnoreCase(cacheType)
                ? new RedisUrlCache("redis://localhost:6379")
                : new InMemoryUrlCache();

        SimpleWebCrawler crawler = new SimpleWebCrawler(startUrl, queue, cache, maxThreads);
        crawler.crawl();
        return 0;
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Application()).execute(args);
        System.exit(exitCode);
    }
}
