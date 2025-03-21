package com.conorsheppard.crawler;

import com.conorsheppard.cache.UrlCache;
import com.conorsheppard.queue.UrlQueue;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Data
public class SimpleWebCrawler {
    private final ExecutorService executor;
    private final UrlQueue urlQueue;
    private final UrlCache urlCache;
    private final Set<String> visitedUrls = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final AtomicInteger activeCrawlers = new AtomicInteger(0);
    private final String baseDomain;

    public SimpleWebCrawler(String startUrl, UrlQueue urlQueue, UrlCache urlCache, int maxThreads) {
        this.executor = Executors.newFixedThreadPool(maxThreads);
        this.urlQueue = urlQueue;
        this.urlCache = urlCache;
        this.baseDomain = getDomain(startUrl);
        enqueueUrl(normalizeUrl(startUrl));
    }

    public void crawl() {
        while (!urlQueue.isEmpty() || activeCrawlers.get() > 0) {
            if (!urlQueue.isEmpty()) {
                String url = urlQueue.dequeue();
                activeCrawlers.getAndIncrement();
                submitCrawl(url);
            }
        }
        shutdownAndAwait();
    }

    private void submitCrawl(String url) {
        executor.submit(() -> {
            crawl(url);
            activeCrawlers.getAndDecrement();
        });
    }

    private void crawl(String url) {
        if (!visitedUrls.add(url)) return;

        if (!isHtmlContent(url)) {
            log.info("Skipping non-HTML URL: {}", url);
            return;
        }

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
            log.info("  urlCache size: {}", urlCache.size());
            log.info("  urlQueue size: {}", urlQueue.size());
        } catch (Exception e) {
            log.error("Failed to crawl: {}", url, e);
        }
    }

    private void enqueueUrl(String url) {
        if (urlCache.add(url)) urlQueue.enqueue(url);
    }

    public boolean isHtmlContent(String url) {
        try {
            Connection.Response response = Jsoup.connect(url).method(Connection.Method.HEAD).execute();
            String contentType = response.contentType();
            return contentType != null && contentType.startsWith("text/html");
        } catch (IOException e) {
            log.warn("HEAD request failed for: {}, {}", url, e.getMessage());
            return false;
        }
    }

    public boolean isValidUrl(String url) {
        return url.startsWith("http") && getDomain(url).equals(baseDomain);
    }

    public String getDomain(String url) {
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

    public void shutdownAndAwait() {
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
