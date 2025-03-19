package com.conorsheppard;

import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URI;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

@Slf4j
@Data
public class SimpleWebCrawler {
    private final Set<String> visitedUrls = new HashSet<>();
    private final Queue<String> queue = new LinkedList<>();
    private final Set<String> processedURLs = new HashSet<>();
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
        new SimpleWebCrawler(args[0]).crawl();
    }

    public void crawl() {
        int totalScrapedCount = 0;
        while (!queue.isEmpty()) {
            String url = queue.poll();
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
                        if (!visitedUrls.contains(nextUrl)) {
                            enqueueUrl(nextUrl);
                        }
                    }
                }

                log.info("  Total links scraped: {}", totalScrapedCount);
                log.info("  Total unique links processed: {}", visitedUrls.size());
                log.info("  Current queue size: {}", queue.size());
            } catch (Exception e) {
                log.error("Failed to crawl: {}", url);
            }
        }
    }

    private void enqueueUrl(String url) {
        if (!processedURLs.contains(url)) {
            queue.add(url);
            processedURLs.add(url);
        }
    }

    boolean isValidUrl(String url) {
        return url.startsWith("http") && getDomain(url).equals(baseDomain) && !visitedUrls.contains(url);
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
