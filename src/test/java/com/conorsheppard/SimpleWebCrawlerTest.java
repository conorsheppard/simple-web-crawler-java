package com.conorsheppard;

import com.conorsheppard.cache.InMemoryUrlCache;
import com.conorsheppard.cache.UrlCache;
import com.conorsheppard.crawler.SimpleWebCrawler;
import com.conorsheppard.queue.ConcurrentQueue;
import com.conorsheppard.queue.UrlQueue;
import com.conorsheppard.web.WebClient;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.conorsheppard.crawler.SimpleWebCrawler.normalizeUrl;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SimpleWebCrawlerTest {
    private SimpleWebCrawler crawler;

    @Mock
    private WebClient mockWebClient;

    @SneakyThrows
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        crawler = new SimpleWebCrawler("https://example.com", new ConcurrentQueue(), new InMemoryUrlCache(),
                Executors.newSingleThreadExecutor(), TerminalBuilder.terminal(), mockWebClient);
    }

    @Test
    void testConstructor() {
        assertEquals("example.com", crawler.getBaseDomain());
        assertTrue(crawler.getUrlQueue().contains("https://example.com"));
    }

    @Test
    void testGetDomain() {
        assertEquals("example.com", crawler.getDomain("http://example.com"));
        assertEquals("example.com", crawler.getDomain("https://example.com/page"));
        assertEquals("sub.example.com", crawler.getDomain("https://sub.example.com"));
        assertNotEquals("example.com", crawler.getDomain("https://other.com"));
        assertNull(crawler.getDomain("invalid-url"));
    }

    @Test
    void testIsValidUrl() {
        assertFalse(crawler.isValidUrl("example.com"));
        assertTrue(crawler.isValidUrl("http://example.com")); // Non-HTTPS
        assertTrue(crawler.isValidUrl("https://example.com")); // Base URL
        assertTrue(crawler.isValidUrl("https://example.com/page"));
        assertFalse(crawler.isValidUrl("https://other.com/page")); // Different domain
        assertFalse(crawler.isValidUrl("ftp://example.com/file")); // Non-HTTP
        assertFalse(crawler.isValidUrl("https://subdomain.example.org")); // Subdomain
    }

    @Test
    void testNormalizeUrl() {
        // URLs with and without fragments should be treated the same
        assertEquals("https://monzo.com/supporting-customers",
                normalizeUrl("https://monzo.com/supporting-customers/#mainContent"));

        assertEquals("https://monzo.com/supporting-customers",
                normalizeUrl("https://monzo.com/supporting-customers#"));

        assertEquals("https://monzo.com/supporting-customers",
                normalizeUrl("https://monzo.com/supporting-customers"));

        // URLs with and without trailing slashes should be treated the same
        assertEquals("https://monzo.com/supporting-customers",
                normalizeUrl("https://monzo.com/supporting-customers/"));

        // Test multiple trailing slashes
        assertEquals("https://example.com/page",
                SimpleWebCrawler.normalizeUrl("https://example.com/page///"));

        assertEquals("https://monzo.com/supporting-customers",
                normalizeUrl("https://monzo.com/supporting-Customers/"));

        // Ensure different paths are not mistakenly treated as the same
        assertNotEquals("https://monzo.com/supporting-customers",
                normalizeUrl("https://monzo.com/another-path"));

        assertEquals("https://monzo.com/supporting customers",
                normalizeUrl("https://monzo.com/supporting customers"));
    }

    // This test is more about ensuring the logic is correct rather than testing the queue itself
    // Since the queue is a ConcurrentLinkedQueue, it's hard to test its internal state directly
    @SneakyThrows
    @Test
    void testEnqueueUrl() {
        // Get initial state
        int initialQueueSize = crawler.getUrlQueue().size();
        int initialCacheSize = crawler.getUrlCache().size();


        // Access the private method using reflection
        Method enqueueUrlMethod = crawler.getClass().getDeclaredMethod("enqueueUrl", String.class);
        enqueueUrlMethod.setAccessible(true); // This allows access to the private method

        // Invoke the private method
        enqueueUrlMethod.invoke(crawler, "https://example.com/new-page");

        // Verify queue and cache were updated
        assertEquals(initialQueueSize + 1, crawler.getUrlQueue().size());
        assertEquals(initialCacheSize + 1, crawler.getUrlCache().size());
        assertTrue(crawler.getUrlCache().contains("https://example.com/new-page"));

        // Enqueue same URL again
        enqueueUrlMethod.invoke(crawler, "https://example.com/new-page");

        // Verify duplicate was not added
        assertEquals(initialQueueSize + 1, crawler.getUrlQueue().size());
        assertEquals(initialCacheSize + 1, crawler.getUrlCache().size());
    }

    @SneakyThrows
    @Test
    void testVisitedUrlsHandling() {
        // Add URL to visited set
        crawler.getVisitedUrlSet().add("https://example.com/visited");

        Method crawlMethod = crawler.getClass().getDeclaredMethod("enqueueUrl", String.class);
        crawlMethod.setAccessible(true); // This allows access to the private method

        // Attempt to crawl visited URL
        crawlMethod.invoke(crawler, "https://example.com/new-page");

        // Verify visited URL was not processed again
        // crawler is already seeded with https://example.com + this one (https://example.com/visited) == 2
        assertEquals(1, crawler.getVisitedUrlSet().size());
    }

    @Test
    void testShutdownAndAwait() {
        // Create a spy on the executor to verify shutdown is called
        ExecutorService executorSpy = spy(crawler.getExecutor());

        // Use reflection to replace the executor field with the spy
        try {
            Field executorField = SimpleWebCrawler.class.getDeclaredField("executor");
            executorField.setAccessible(true);
            executorField.set(crawler, executorSpy);
        } catch (Exception e) {
            fail("Failed to set up test: " + e.getMessage());
        }

        // Call shutdownAndAwait
        crawler.shutdownAndAwait();

        // Verify shutdown was called
        verify(executorSpy).shutdown();

        // Verify the executor is marked as shutdown
        assertTrue(executorSpy.isShutdown());
    }

    @SneakyThrows
    @Test
    void testWhenResponseIsJson_isHtmlContentReturnsFalse() {
        Connection.Response mockResponse = mock(Connection.Response.class);
        when(mockResponse.contentType()).thenReturn("application/json");
        when(mockWebClient.head("https://example.com")).thenReturn(mockResponse);
        assertFalse(crawler.isHtmlContent("https://example.com"));
    }

    @SneakyThrows
    @Test
    void testWhenResponseIsNull_isHtmlContentReturnsFalse() {
        Connection.Response mockResponse = mock(Connection.Response.class);
        when(mockResponse.contentType()).thenReturn(null);
        when(mockWebClient.head("https://example.com")).thenReturn(mockResponse);
        assertFalse(crawler.isHtmlContent("https://example.com"));
    }

    @SneakyThrows
    @Test
    void testWhenOnceLinkExistsInThePage_FetchIsCalledOnce() {
        Connection.Response mockResponse = mock(Connection.Response.class);
        when(mockResponse.contentType()).thenReturn("text/html");
        when(mockWebClient.head("https://example.com")).thenReturn(mockResponse);
        Document mockFetchResponse = mock(Document.class);
        when(mockResponse.body()).thenReturn("<html><body><a href='https://example.com/page2'>Next</a></body></html>");
        when(mockWebClient.fetch("https://example.com")).thenReturn(mockFetchResponse);
        crawler.crawl();
        verify(mockWebClient, times(1)).fetch("https://example.com");
    }


    @SneakyThrows
    @Test
    void testWhenResponseIsEmpty_isHtmlContentReturnsFalse() {
        Connection.Response mockResponse = mock(Connection.Response.class);
        when(mockResponse.contentType()).thenReturn("");
        when(mockWebClient.head("https://example.com")).thenReturn(mockResponse);
        assertFalse(crawler.isHtmlContent("https://example.com"));
    }

    @SneakyThrows
    @Test
    void testWhenResponseIsValid_isHtmlContentReturnsTrue() {
        Connection.Response mockResponse = mock(Connection.Response.class);
        when(mockResponse.contentType()).thenReturn("text/html; charset=UTF-8");
        when(mockWebClient.head("https://example.com")).thenReturn(mockResponse);
        assertTrue(crawler.isHtmlContent("https://example.com"));
    }

    @SneakyThrows
    @Test
    void testWhenWebClientThrowsIOException_isHtmlContentCatchesAndReturnsFalse() {
        when(mockWebClient.head("https://example.com")).thenThrow(new IOException());
        assertFalse(crawler.isHtmlContent("https://example.com"));
    }

//    @Test
//    void testCrawlUsesThreadPool() {
//        ExecutorService mockExecutor = mock(ExecutorService.class);
//        WebClient mockWebClient = mock(WebClient.class);
//
//        UrlQueue queue = new ConcurrentQueue();
//        UrlCache cache = new InMemoryUrlCache();
//
//        SimpleWebCrawler simpleWebCrawler = new SimpleWebCrawler(
//                "https://example.com", queue, cache,
//                mockExecutor, new ProgressBarStub(), mockWebClient
//        );
//
//        queue.enqueue("https://example.com");
//        simpleWebCrawler.crawl();
//
//        verify(mockExecutor, atLeastOnce()).submit(any(Runnable.class));
//    }
}
