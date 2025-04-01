package com.conorsheppard;

import com.conorsheppard.cache.InMemoryUrlCache;
import com.conorsheppard.cache.RedisUrlCache;
import com.conorsheppard.cache.UrlCache;
import com.conorsheppard.crawler.SimpleWebCrawler;
import com.conorsheppard.queue.ConcurrentQueue;
import com.conorsheppard.queue.KafkaQueue;
import com.conorsheppard.queue.UrlQueue;
import com.conorsheppard.web.JSoupWebClient;
import io.lettuce.core.RedisClient;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jline.terminal.TerminalBuilder;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import static picocli.CommandLine.Parameters;

@Slf4j
@Command(name = "WebCrawler", mixinStandardHelpOptions = true, version = "1.0",
        description = "A simple web crawler with configurable queue and cache options.")
public class Application implements Callable<Integer> {

    @Parameters(index = "0", description = "The website URL to crawl.", arity = "0..1")
    private String baseURL;

    @Option(names = {"-c", "--concurrent"}, description = "Uses an in-memory queue and cache", defaultValue = "true")
    private boolean isConcurrent;

    @Option(names = {"-d", "--dist", "--distributed"}, description = "Uses Kafka & Redis for distributed crawling", defaultValue = "false")
    private boolean isDistributed;

    @Option(names = {"-t", "--threads"}, description = "Max number of threads", defaultValue = "30")
    private int maxThreads;

    @SneakyThrows
    @Override
    public Integer call() {
        getBaseURL();
        if (baseURL.isEmpty()) return 1;
        UrlQueue queue = getQueue();
        UrlCache cache = getCache();
        logCrawlerInfo();
        SimpleWebCrawler crawler = new SimpleWebCrawler(baseURL, queue, cache, Executors.newFixedThreadPool(maxThreads),
                TerminalBuilder.builder().dumb(true).build(), new JSoupWebClient());
        crawler.crawl();
        askToPrintUrls(crawler);
        return 0;
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Application()).execute(args);
        System.exit(exitCode);
    }

    private void askToPrintUrls(SimpleWebCrawler crawler) {
        Scanner scanner = new Scanner(System.in);
        log.info("Do you want to see all the URLs? (Y/n): ");
        String input = scanner.nextLine().trim().toLowerCase();
        if (input.isEmpty() || input.equals("y")) {
            new ArrayList<>(crawler.getVisitedUrlSet()).reversed().forEach(url -> log.info("{}", url));
        }
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
        return isDistributed ? new KafkaQueue() : new ConcurrentQueue();
    }

    private UrlCache getCache() {
        String redisUri = System.getenv().getOrDefault("ENVIRONMENT", "prod").equals("dev")
                ? "redis://localhost:6379" : "redis://redis-web-crawler:6379";
        return isDistributed ? new RedisUrlCache(RedisClient.create(redisUri).connect())
                : new InMemoryUrlCache();
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
                isDistributed ? "kafka" : "concurrentQueue",
                isDistributed ? "redis" : "inMemory",
                maxThreads);

    }
}
