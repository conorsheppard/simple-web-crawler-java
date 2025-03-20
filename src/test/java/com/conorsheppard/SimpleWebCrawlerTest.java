package com.conorsheppard;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.concurrent.ExecutorService;

import static com.conorsheppard.SimpleWebCrawler.normalizeUrl;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SimpleWebCrawlerTest {
    private SimpleWebCrawler crawler;

    @BeforeEach
    void setUp() {
        crawler = new SimpleWebCrawler("https://example.com");
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
    }

    // This test is more about ensuring the logic is correct rather than testing the queue itself
    // Since the queue is a ConcurrentLinkedQueue, it's hard to test its internal state directly
    @Test
    void testEnqueueUrl() {
        // Get initial state
        int initialQueueSize = crawler.getUrlQueue().size();
        int initialCacheSize = crawler.getUrlCache().size();

        // Enqueue new URL
        crawler.enqueueUrl("https://example.com/new-page");

        // Verify queue and cache were updated
        assertEquals(initialQueueSize + 1, crawler.getUrlQueue().size());
        assertEquals(initialCacheSize + 1, crawler.getUrlCache().size());
        assertTrue(crawler.getUrlCache().contains("https://example.com/new-page"));

        // Enqueue same URL again
        crawler.enqueueUrl("https://example.com/new-page");

        // Verify duplicate was not added
        assertEquals(initialQueueSize + 1, crawler.getUrlQueue().size());
        assertEquals(initialCacheSize + 1, crawler.getUrlCache().size());
    }

    @Test
    void testVisitedUrlsHandling() {
        // Add URL to visited set
        crawler.getVisitedUrls().add("https://example.com/visited");

        // Attempt to crawl visited URL
        crawler.crawl("https://example.com/visited");

        // Verify visited URL was not processed again
        // crawler is already seeded with https://example.com + this one (https://example.com/visited) == 2
        assertEquals(2, crawler.getVisitedUrls().size());
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

    @Test
    void testIsHtmlContent() throws IOException {
        // Setup mock for Jsoup
        Connection mockConnection = mock(Connection.class);
        Connection.Response mockResponse = mock(Connection.Response.class);

        try (MockedStatic<Jsoup> jsoupMock = Mockito.mockStatic(Jsoup.class)) {
            jsoupMock.when(() -> Jsoup.connect(anyString())).thenReturn(mockConnection);
            when(mockConnection.method(any(Connection.Method.class))).thenReturn(mockConnection);
            when(mockConnection.execute()).thenReturn(mockResponse);

            // Test HTML content
            when(mockResponse.contentType()).thenReturn("text/html; charset=UTF-8");
            assertTrue(crawler.isHtmlContent("https://example.com"));

            // Test non-HTML content
            when(mockResponse.contentType()).thenReturn("application/pdf");
            assertFalse(crawler.isHtmlContent("https://example.com/document.pdf"));

            // Test null content type
            when(mockResponse.contentType()).thenReturn(null);
            assertFalse(crawler.isHtmlContent("https://example.com/unknown"));
        }
    }

    @Test
    void testIsHtmlContentWithException() throws IOException {
        // Set up mock for Jsoup that throws exception
        try (MockedStatic<Jsoup> jsoupMock = Mockito.mockStatic(Jsoup.class)) {
            Connection mockConnection = mock(Connection.class);
            jsoupMock.when(() -> Jsoup.connect(anyString())).thenReturn(mockConnection);
            when(mockConnection.method(any(Connection.Method.class))).thenReturn(mockConnection);
            when(mockConnection.execute()).thenThrow(new IOException("Connection failed"));

            assertFalse(crawler.isHtmlContent("https://example.com/error"));
        }
    }
}
