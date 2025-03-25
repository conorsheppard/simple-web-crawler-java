package com.conorsheppard;

import com.conorsheppard.cache.InMemoryUrlCache;
import com.conorsheppard.cache.UrlCache;
import com.conorsheppard.crawler.SimpleWebCrawler;
import com.conorsheppard.queue.ConcurrentQueue;
import com.conorsheppard.queue.UrlQueue;
import com.conorsheppard.web.WebClient;
import lombok.SneakyThrows;
import org.jline.terminal.TerminalBuilder;
import org.jsoup.Connection;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.conorsheppard.crawler.SimpleWebCrawler.normalizeUrl;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class SimpleWebCrawlerTest {
    private SimpleWebCrawler crawler;
    private static final String EXAMPLE_URL = "https://example.com";

    @Mock
    private WebClient mockWebClient;

    @SneakyThrows
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        crawler = new SimpleWebCrawler(EXAMPLE_URL, new ConcurrentQueue(), new InMemoryUrlCache(),
                Executors.newSingleThreadExecutor(), TerminalBuilder.terminal(), mockWebClient);
    }

    @Test
    void testConstructor() {
        assertEquals("example.com", crawler.getBaseDomain());
        assertTrue(crawler.getUrlQueue().contains(EXAMPLE_URL));
    }

    @Test
    void testGetDomain() {
        assertEquals("example.com", crawler.getDomain("http://example.com"));
        assertEquals("example.com", crawler.getDomain("https://example.com/page"));
        assertEquals("sub.example.com", crawler.getDomain("https://sub.example.com"));
        assertNotEquals("example.com", crawler.getDomain("https://other.com"));
        assertNull(crawler.getDomain("invalid-url"));
        assertEquals("", crawler.getDomain("http://exa<mple.com"));
    }


    @ParameterizedTest
    @CsvSource({
            "'example.com', false",
            "'http://example.com', true",
            "'https://example.com', true",
            "'https://example.com/page', true",
            "'https://other.com/page', false",
            "'ftp://example.com/file', false",
            "'https://subdomain.example.org', false"
    })
    @DisplayName("Test for validation of URLs, i.e. starts with 'http', includes the base domain and is not an ignored file type")
    void testIsValidUrl(String url, boolean expected) {
        assertEquals(expected, crawler.isValidUrl(url));
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

        assertThrows(URISyntaxException.class, () -> normalizeUrl("http://exa<mple.com"));
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
        when(mockWebClient.head(EXAMPLE_URL)).thenReturn(mockResponse);
        assertFalse(crawler.isHtmlContent(EXAMPLE_URL));
    }

    @SneakyThrows
    @Test
    void testWhenResponseIsNull_isHtmlContentReturnsFalse() {
        Connection.Response mockResponse = mock(Connection.Response.class);
        when(mockResponse.contentType()).thenReturn(null);
        when(mockWebClient.head(EXAMPLE_URL)).thenReturn(mockResponse);
        assertFalse(crawler.isHtmlContent(EXAMPLE_URL));
    }

    @SneakyThrows
    @Test
    void testWhenOnceLinkExistsInThePage_FetchIsCalledOnce() {
        Connection.Response mockResponse = mock(Connection.Response.class);
        when(mockResponse.contentType()).thenReturn("text/html");
        when(mockWebClient.head(EXAMPLE_URL)).thenReturn(mockResponse);
        Document mockFetchResponse = mock(Document.class);
        when(mockResponse.body()).thenReturn("<html><body><a href='https://example.com/page2'>Next</a></body></html>");
        when(mockWebClient.fetch(EXAMPLE_URL)).thenReturn(mockFetchResponse);
        crawler.crawl();
        verify(mockWebClient, times(1)).fetch(EXAMPLE_URL);
    }


    @SneakyThrows
    @Test
    void testWhenResponseIsEmpty_isHtmlContentReturnsFalse() {
        Connection.Response mockResponse = mock(Connection.Response.class);
        when(mockResponse.contentType()).thenReturn("");
        when(mockWebClient.head(EXAMPLE_URL)).thenReturn(mockResponse);
        assertFalse(crawler.isHtmlContent(EXAMPLE_URL));
    }

    @SneakyThrows
    @Test
    void testWhenResponseIsValid_isHtmlContentReturnsTrue() {
        Connection.Response mockResponse = mock(Connection.Response.class);
        when(mockResponse.contentType()).thenReturn("text/html; charset=UTF-8");
        when(mockWebClient.head(EXAMPLE_URL)).thenReturn(mockResponse);
        assertTrue(crawler.isHtmlContent(EXAMPLE_URL));
    }

    @SneakyThrows
    @Test
    void testWhenWebClientThrowsIOException_isHtmlContentCatchesAndReturnsFalse() {
        when(mockWebClient.head(EXAMPLE_URL)).thenThrow(new IOException());
        assertFalse(crawler.isHtmlContent(EXAMPLE_URL));
    }

    @SneakyThrows
    @Test
    void testCrawlUsesThreadPool() {
        ExecutorService realExecutor = Executors.newSingleThreadExecutor();
        ExecutorService spyExecutor = spy(realExecutor);

        SimpleWebCrawler simpleWebCrawler = new SimpleWebCrawler(
                EXAMPLE_URL, new ConcurrentQueue(), new InMemoryUrlCache(),
                spyExecutor, TerminalBuilder.terminal(), mockWebClient
        );

        Connection.Response mockResponse = mock(Connection.Response.class);
        when(mockResponse.contentType()).thenReturn("application/json");
        when(mockWebClient.head(EXAMPLE_URL)).thenReturn(mockResponse);

        simpleWebCrawler.crawl();

        verify(spyExecutor, atLeastOnce()).submit(any(Runnable.class));
        verify(spyExecutor).shutdown();
        verify(spyExecutor, atLeastOnce()).awaitTermination(anyLong(), any(TimeUnit.class));
    }

    @SneakyThrows
    @Test
    void testWhenShutDown_executorAwaitsTermination() {
        // Create a mock executor service
        ExecutorService mockExecutor = mock(ExecutorService.class);

        // Simulate two iterations before terminating
        when(mockExecutor.awaitTermination(anyLong(), any(TimeUnit.class)))
                .thenReturn(false) // First iteration (while loop continues)
                .thenReturn(true); // Second iteration (loop exits)

        SimpleWebCrawler simpleWebCrawler = new SimpleWebCrawler(EXAMPLE_URL, new ConcurrentQueue(),
                new InMemoryUrlCache(), mockExecutor, TerminalBuilder.terminal(), mockWebClient
        );

        // Call shutdownAndAwait
        simpleWebCrawler.shutdownAndAwait();

        // Verify shutdown() was called
        verify(mockExecutor).shutdown();

        // Verify awaitTermination() was called at least twice (loop runs)
        verify(mockExecutor, times(2)).awaitTermination(anyLong(), any(TimeUnit.class));
    }

    @SneakyThrows
    @Test
    void testShutdownAndAwaitHandlesInterruptedException() {
        // Create a mock executor service
        ExecutorService mockExecutor = mock(ExecutorService.class);

        // Simulate an InterruptedException
        when(mockExecutor.awaitTermination(anyLong(), any(TimeUnit.class)))
                .thenThrow(new InterruptedException());

        SimpleWebCrawler simpleWebCrawler = new SimpleWebCrawler(
                EXAMPLE_URL, new ConcurrentQueue(), new InMemoryUrlCache(),
                mockExecutor, TerminalBuilder.terminal(), mockWebClient
        );

        // Call shutdownAndAwait
        simpleWebCrawler.shutdownAndAwait();

        // Verify shutdown() was called
        verify(mockExecutor).shutdown();

        // Verify awaitTermination() was called at least once
        verify(mockExecutor, atLeastOnce()).awaitTermination(anyLong(), any(TimeUnit.class));

        // Verify that the interrupted flag was set on the current thread
        assertTrue(Thread.currentThread().isInterrupted());
    }
}
