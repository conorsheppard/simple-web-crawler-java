package com.conorsheppard.crawler;

import com.conorsheppard.cache.UrlCache;
import com.conorsheppard.queue.UrlQueue;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
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
    private final Terminal terminal;


    @SneakyThrows
    public SimpleWebCrawler(String startUrl, UrlQueue urlQueue, UrlCache urlCache, int maxThreads) {
        this.executor = Executors.newFixedThreadPool(maxThreads);
        this.urlQueue = urlQueue;
        this.urlCache = urlCache;
        this.baseDomain = getDomain(startUrl);
        this.terminal = TerminalBuilder.terminal();
        enqueueUrl(normalizeUrl(startUrl));

        startProgressBar();
    }

    public void crawl() {
        while (!urlQueue.isEmpty() || activeCrawlers.get() > 0) {
            if (!urlQueue.isEmpty()) {
                String url = urlQueue.dequeue();
                submitCrawl(url);
            }
        }
        shutdownAndAwait();
    }

    private void submitCrawl(String url) {
        activeCrawlers.incrementAndGet();
        executor.submit(() -> {
            try {
                crawl(url);
            } finally {
                activeCrawlers.decrementAndGet();
            }
        });
    }

    private void crawl(String url) {
        if (!visitedUrls.add(url)) return;

        if (!isHtmlContent(url)) {
            log.debug("Skipping non-HTML URL: {}", url);
            return;
        }

        try {
            Document doc = Jsoup.connect(url).timeout(5000).get();
            Elements links = doc.select("a[href]");

            for (Element link : links) {
                String nextUrl = normalizeUrl(link.absUrl("href"));
                if (isValidUrl(nextUrl) && urlCache.add(nextUrl)) {
                    urlQueue.enqueue(nextUrl);
                }
            }

        } catch (Exception e) {
            log.error("Failed to crawl: {}", url, e);
        }
    }

    public void startProgressBar() {
        new Thread(this::writeProgress).start();
    }

    @SneakyThrows
    void writeProgress() {
        while (!executor.isShutdown()) {
            Thread.sleep(500); // Refresh every 500ms
            int scraped = visitedUrls.size();
            int discovered = urlCache.size();
            int percentage = (discovered == 0) ? 0 : (scraped * 100) / discovered;

            terminal.writer().printf("\r🌍 Crawling: [%s] %d%% (%d/%d URLs)",
                    progressBar(percentage), percentage, scraped, discovered);
            terminal.flush();
        }
    }

    private String progressBar(int percentage) {
        int width = 30; // Width of the bar
        int filled = (percentage * width) / 100;
        return "█".repeat(filled) + "-".repeat(width - filled);
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
        return url.startsWith("http") &&
                getDomain(url).equals(baseDomain) &&
                !isIgnoredFile(url);
    }

    private boolean isIgnoredFile(String url) {
        String lowerUrl = url.toLowerCase();
        return lowerUrl.matches(".*\\.(pdf|jpg|png|gif|mp4|zip|exe|docx|xlsx|pptx|mp3)(\\?.*)?$");
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
        URI uri = new URI(url.trim().replace(" ", "%20"));
        return (uri.getScheme() + "://" + uri.getHost() + uri.getPath())
                .toLowerCase()
                .replaceAll("/+$", "");
    }

    public void shutdownAndAwait() {
        System.out.println();
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
        log.info("total valid URLs processed: {}", urlCache.size());
    }
}
