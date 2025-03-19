package com.conorsheppard;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

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
    void testGetDomain() {
        assertEquals("example.com", crawler.getDomain("https://example.com/page"));
        assertEquals("sub.example.com", crawler.getDomain("https://sub.example.com"));
        assertNotEquals("example.com", crawler.getDomain("https://other.com"));
    }

    @Test
    void testIsValidUrl() {
        assertTrue(crawler.isValidUrl("https://example.com/page"));
        assertFalse(crawler.isValidUrl("https://other.com/page")); // Different domain
        assertFalse(crawler.isValidUrl("ftp://example.com/file")); // Non-HTTP
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

        // Ensure different paths are not mistakenly treated as the same
        assertNotEquals("https://monzo.com/supporting-customers",
                normalizeUrl("https://monzo.com/another-path"));
    }

//    @Test
//    void testCrawlWithMockedPage() throws Exception {
//        // Mock Jsoup's Connection and Document
//        Connection mockConnection = mock(Connection.class);
//        Document mockDocument = mock(Document.class);
//
//        // Mock Jsoup.connect() to return a valid connection
//        when(Jsoup.connect(Mockito.any(String.class))).thenReturn(mockConnection);
//
//        // Ensure the connection.get() returns the mocked document
//        when(mockConnection.get()).thenReturn(mockDocument);
//
//        // If needed, another way to avoid execution:
//        // doReturn(mockConnection).when(Jsoup.class);
//
//        // Create an instance of your web crawler
//        SimpleWebCrawler crawler = new SimpleWebCrawler("http://example.com");
//
//        // Call the method that uses Jsoup
//        crawler.crawl();
//
//        // Verify Jsoup.connect() was called with the correct URL
//        verify(Jsoup.connect("http://example.com")).get();
//    }
}
