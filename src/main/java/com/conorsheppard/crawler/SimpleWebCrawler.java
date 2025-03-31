package com.conorsheppard.crawler;

import com.conorsheppard.cache.UrlCache;
import com.conorsheppard.queue.UrlQueue;
import com.conorsheppard.web.WebClient;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jline.terminal.Terminal;
import org.jsoup.Connection;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Data
public class SimpleWebCrawler {
    private final ExecutorService executor;
    private final UrlQueue urlQueue;
    private final UrlCache urlCache;
    private final Set<String> visitedUrlSet = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final AtomicInteger activeCrawlers = new AtomicInteger(-1);
    private final String baseDomain;
    private final Terminal terminal;
    private final WebClient webClient;

    public SimpleWebCrawler(String startUrl, UrlQueue urlQueue, UrlCache urlCache,
                            ExecutorService executor, Terminal terminal, WebClient webClient) {
        this.executor = executor;
        this.urlQueue = urlQueue;
        this.urlCache = urlCache;
        this.baseDomain = getDomain(startUrl);
        this.terminal = terminal;
        this.webClient = webClient;
        enqueueUrl(normalizeUrl(startUrl));

        startProgressBar();
    }

    public void crawl() {
        // if the queue still has elements or if there are still active crawlers, the queue could be empty but if there
        // are still crawlers doing work, they could be about to push more URLs onto the queue
        while (!urlQueue.isEmpty() || activeCrawlers.get() > 0) {
            if (!urlQueue.isEmpty()) {
                String url = urlQueue.dequeue();
                if (url != null) {
                    log.debug("submitting URL: {}", url);
                    submitCrawl(url);
                }
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
        if (!visitedUrlSet.add(url)) return;

        if (!isHtmlContent(url)) {
            log.debug("Skipping non-HTML URL: {}", url);
            return;
        }

        try {
            Document doc = webClient.fetch(url);
            Elements links = doc.select("a[href]");
            links.forEach(this::normaliseAndEnqueue);
        } catch (IOException e) {
            log.error("Failed to crawl: {}", url, e);
        }
    }

    private void normaliseAndEnqueue(Element link) {
        String nextUrl = normalizeUrl(link.absUrl("href"));
        if (isValidUrl(nextUrl)) enqueueUrl(nextUrl);
    }

    public void startProgressBar() {
        new Thread(this::writeProgress).start();
    }

    @SneakyThrows
    void writeProgress() {
        while (!executor.isShutdown()) {
            Thread.sleep(500); // Refresh every 500ms
            int scraped = visitedUrlSet.size();
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
            Connection.Response response = webClient.head(url);
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
