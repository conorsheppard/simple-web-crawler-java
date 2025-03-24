package com.conorsheppard;

import com.conorsheppard.cache.InMemoryUrlCache;
import com.conorsheppard.cache.RedisUrlCache;
import com.conorsheppard.cache.UrlCache;
import com.conorsheppard.crawler.SimpleWebCrawler;
import com.conorsheppard.queue.ConcurrentQueue;
import com.conorsheppard.queue.KafkaQueue;
import com.conorsheppard.queue.UrlQueue;
import com.conorsheppard.web.JSoupWebClient;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jline.terminal.TerminalBuilder;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import static picocli.CommandLine.*;

@Slf4j
@Command(name = "WebCrawler", mixinStandardHelpOptions = true, version = "1.0",
        description = "A simple web crawler with configurable queue and cache options.")
public class Application implements Callable<Integer> {

    @Parameters(index = "0", description = "The website URL to crawl.", arity = "0..1")
    private String baseURL;

    @Option(names = {"-q", "--queue"}, description = "Queue type (concurrentQueue, kafka)", defaultValue = "concurrentQueue")
    private String queueType;

    @Option(names = {"-c", "--cache"}, description = "Cache type (inMemory, redis)", defaultValue = "inMemory")
    private String cacheType;

    @Option(names = {"-t", "--threads"}, description = "Max number of threads", defaultValue = "30")
    private int maxThreads;

    @SneakyThrows
    @Override
    public Integer call() {
        getBaseURL();
        if (baseURL.isEmpty()) return 1;
        UrlQueue queue = getQueue();
        UrlCache cache = getCache();
        SimpleWebCrawler crawler = new SimpleWebCrawler(baseURL, queue, cache, Executors.newFixedThreadPool(maxThreads),
                TerminalBuilder.terminal(), new JSoupWebClient());
        logCrawlerInfo();
        crawler.crawl();
        return 0;
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Application()).execute(args);
        System.exit(exitCode);
    }

    private void getBaseURL() {
        if (baseURL == null || baseURL.isEmpty()) {
            Scanner scanner = new Scanner(System.in);
            log.info("Enter the URL to scrape: ");
            baseURL = scanner.nextLine().trim();
        }

        if (baseURL.isEmpty()) {
            log.error("No URL provided. Exiting...");
        }
    }

    private UrlQueue getQueue() {
        return isKafka() ? new KafkaQueue() : new ConcurrentQueue();
    }

    private UrlCache getCache() {
        return isRedis() ? new RedisUrlCache("redis://localhost:6379") : new InMemoryUrlCache();
    }

    public void logCrawlerInfo() {
        log.info("""
                        
                        🚀 Running web crawler...
                        🔗 URL: {}
                        🗂 Queue Type: {}
                        🛠 Cache Type: {}
                        ⚡ Threads: {}
                        """,
                baseURL,
                isKafka() ? "kafka" : "concurrentQueue",
                isRedis() ? "redis" : "inMemory",
                maxThreads);

    }

    private boolean isKafka() {
        return "kafka".equalsIgnoreCase(queueType);
    }

    private boolean isRedis() {
        return "redis".equalsIgnoreCase(cacheType);
    }
}
