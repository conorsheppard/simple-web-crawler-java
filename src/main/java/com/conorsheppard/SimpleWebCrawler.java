package com.conorsheppard;

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
public class SimpleWebCrawler {
    private final Set<String> visitedUrls = new HashSet<>();
    private final Queue<String> queue = new LinkedList<>();
    private final String baseDomain;

    public SimpleWebCrawler(String startUrl) {
        this.baseDomain = getDomain(startUrl);
        queue.add(startUrl);
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            log.info("Usage: SimpleWebCrawler <start-url>");
            return;
        }
        new SimpleWebCrawler(args[0]).crawl();
    }

    public void crawl() {
        while (!queue.isEmpty()) {
            String url = queue.poll();
            if (visitedUrls.contains(url)) {
                continue;
            }
            visitedUrls.add(url);
            log.info("Visiting: {}", url);

            try {
                Document doc = Jsoup.connect(url).get();
                Elements links = doc.select("a[href]");

                for (Element link : links) {
                    String nextUrl = link.absUrl("href");
                    if (isValidUrl(nextUrl)) {
                        log.info("  Found: {}", nextUrl);
                        queue.add(nextUrl);
                    }
                }
            } catch (Exception e) {
                log.error("Failed to crawl: {}", url);
            }
        }
    }

    private boolean isValidUrl(String url) {
        return url.startsWith("http") && getDomain(url).equals(baseDomain) && !visitedUrls.contains(url);
    }

    private String getDomain(String url) {
        try {
            return new URI(url).getHost();
        } catch (Exception e) {
            return "";
        }
    }
}
