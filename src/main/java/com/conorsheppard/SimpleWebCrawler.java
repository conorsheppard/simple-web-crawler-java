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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Data
public class SimpleWebCrawler {
    private final ExecutorService executor = Executors.newFixedThreadPool(15);
    private final Queue<String> urlQueue = new ConcurrentLinkedQueue<>();
    private final Set<String> visitedUrls = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<String> urlCache = Collections.newSetFromMap(new ConcurrentHashMap<>());
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
        while (!urlQueue.isEmpty()) executor.submit(this::crawl);
        executor.shutdown();
    }


    public void crawl() {
        int totalScrapedCount = 0;
        while (!urlQueue.isEmpty()) {
            String url = urlQueue.poll();
            visitedUrls.add(url);
            log.info("Visiting: {}", url);

            try {
                Document doc = Jsoup.connect(url).get();
                Elements links = doc.select("a[href]");

                for (Element link : links) {
                    totalScrapedCount++;
                    String nextUrl = normalizeUrl(link.absUrl("href"));
                    if (isValidUrl(nextUrl)) {
                        log.info("  Found: {}", nextUrl);
                        if (!urlCache.contains(nextUrl)) {
                            enqueueUrl(nextUrl);
                        }
                    }
                }

                log.info("  Total links scraped: {}", totalScrapedCount);
                log.info("  Total unique links processed: {}", visitedUrls.size());
                log.info("  Current queue size: {}", urlQueue.size());
            } catch (Exception e) {
                log.error("Failed to crawl: {}", url);
            }
        }
    }

    private void enqueueUrl(String url) {
        if (!urlCache.contains(url)) {
            urlQueue.add(url);
            urlCache.add(url);
        }
    }

    boolean isValidUrl(String url) {
        return url.startsWith("http") && getDomain(url).equals(baseDomain) && !urlCache.contains(url);
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
        String normalized = uri.getScheme() + "://" + uri.getHost() + uri.getPath();
        return normalized.replaceAll("/+$", "");
    }
}
