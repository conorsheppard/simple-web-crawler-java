package com.conorsheppard;

import com.conorsheppard.cache.InMemoryUrlCache;
import com.conorsheppard.cache.UrlCache;
import com.conorsheppard.queue.ConcurrentQueue;
import com.conorsheppard.queue.UrlQueue;
import com.conorsheppard.web.JSoupWebClient;
import com.conorsheppard.web.WebClient;
import com.github.tomakehurst.wiremock.WireMockServer;
import lombok.SneakyThrows;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WebCrawlerIntegrationTest {
    private WireMockServer wireMockServer;
    private UrlQueue queue;
    private UrlCache cache;
    private WebClient webClient;

    @BeforeEach
    void setUp() {
        // Start WireMock to mock web pages
        wireMockServer = new WireMockServer(8089);
        wireMockServer.start();
        configureFor("localhost", 8089);

        // Mock Web Pages
        stubFor(get(urlEqualTo("/page1"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/html")
                        .withBody("<html><head><title>Test Page</title></head><body><a href='/page2'>Next</a></body></html>")));

        stubFor(get(urlEqualTo("/page2"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/html")
                        .withBody("<html><head><title>Page 2</title></head><body>End</body></html>")));

        queue = new ConcurrentQueue();
        cache = new InMemoryUrlCache();
        webClient = new JSoupWebClient();
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    @SneakyThrows
    @Test
    void testWebCrawlerIntegration() {
        String startUrl = "http://localhost:8089/page1";

        // Enqueue starting URL
        queue.enqueue(startUrl);

        // Simulate the crawler consuming messages
        String urlToVisit = queue.dequeue();
        assertNotNull(urlToVisit);

        // Fetch the page
        Document doc = webClient.fetch(urlToVisit);
        assertEquals("Test Page", doc.title());

        // Extract and enqueue new links
        String newUrl = "http://localhost:8089/page2";
        queue.enqueue(newUrl);

        // Mark the visited URLs
        cache.add(urlToVisit);

        // Ensure the URL is in the cache
        assertTrue(cache.contains(urlToVisit));

        // Consume the next URL
        String nextUrl = queue.dequeue();
        assertEquals(newUrl, nextUrl);

        // Verify second page loads correctly
        Document secondDoc = webClient.fetch(nextUrl);
        assertEquals("Page 2", secondDoc.title());
    }
}

