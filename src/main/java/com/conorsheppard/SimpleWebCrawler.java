package com.conorsheppard;

import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URI;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Data
public class SimpleWebCrawler {
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
//    private final ExecutorService executor = Executors.newFixedThreadPool(50);
    private final Queue<String> urlQueue = new ConcurrentLinkedQueue<>();
    private final Set<String> visitedUrls = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<String> urlCache = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final AtomicInteger countDownLock = new AtomicInteger(0);
    private final String baseDomain;

    public SimpleWebCrawler(String startUrl) {
        this.baseDomain = getDomain(startUrl);
        enqueueUrl(normalizeUrl(startUrl));
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            log.info("Usage: SimpleWebCrawler <start-url>");
            return;
        }
        new SimpleWebCrawler(args[0]).startCrawling();
    }

    private void startCrawling() {
        while (!urlQueue.isEmpty() || countDownLock.get() > 0) {
            if (!urlQueue.isEmpty()) {
                String url = urlQueue.poll();
                countDownLock.getAndIncrement();
                submitCrawl(url);
            }
        }
        shutdownAndAwait();
    }

    private void submitCrawl(String url) {
        executor.submit(() -> {
            crawl(url);
            countDownLock.getAndDecrement();
        });
    }

    public void crawl(String url) {
        if (!visitedUrls.add(url)) return; // Avoid reprocessing

        log.info("Visiting: {}", url);
        try {
            Document doc = Jsoup.connect(url).get();
            Elements links = doc.select("a[href]");

            for (Element link : links) {
                String nextUrl = normalizeUrl(link.absUrl("href"));
                if (isValidUrl(nextUrl)) {
                    log.info("|----> Found: {}", nextUrl);
                    enqueueUrl(nextUrl);
                }
            }

            log.info("  visitedUrls size: {}", visitedUrls.size());
            log.info("  urlQueue size: {}", urlQueue.size());
            log.info("  urlCache size: {}", urlCache.size());
        } catch (Exception e) {
            log.error("Failed to crawl: {}", url, e);
        }
    }

    private void enqueueUrl(String url) {
        if (urlCache.add(url)) { // Ensures unique URLs are enqueued
            urlQueue.add(url);
            countDownLock.getAndIncrement();
            submitCrawl(url);
        }
    }

    boolean isValidUrl(String url) {
        return url.startsWith("http") && getDomain(url).equals(baseDomain);
    }

    String getDomain(String url) {
        try {
            return new URI(url).getHost();
        } catch (Exception e) {
            return "";
        }
    }

    @SneakyThrows
    public static String normalizeUrl(String url) {
        URI uri = new URI(url);
        return (uri.getScheme() + "://" + uri.getHost() + uri.getPath())
                .toLowerCase()
                .replaceAll("/+$", "");
    }

    private void shutdownAndAwait() {
        log.info("Awaiting shutdown ...");
        executor.shutdown();
        try {
            while (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
                log.info("Waiting for crawling to complete...");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while waiting for completion", e);
        }
        log.info("Crawling complete.");
        log.info("total links processed: {}", urlCache.size());
    }
}
