package com.conorsheppard.web;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.jsoup.Connection;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

class JSoupWebClientTest {
    private static WireMockServer wireMockServer;
    private JSoupWebClient webClient;

    @BeforeAll
    static void setupWireMock() {
        wireMockServer = new WireMockServer(8089); // Start WireMock on port 8089
        wireMockServer.start();
        configureFor("localhost", 8089);
    }

    @AfterAll
    static void stopWireMock() {
        wireMockServer.stop();
    }

    @BeforeEach
    void setUp() {
        webClient = new JSoupWebClient();
    }

    @Test
    void testFetch() throws IOException {
        stubFor(get(urlEqualTo("/test-page"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/html")
                        .withBody("<html><head><title>Test Page</title></head><body>Hello</body></html>")));

        Document document = webClient.fetch("http://localhost:8089/test-page");

        assertNotNull(document);
        assertEquals("Test Page", document.title());
        assertTrue(document.body().text().contains("Hello"));
    }

    @Test
    void testHead() throws IOException {
        stubFor(head(urlEqualTo("/test-page"))
                .willReturn(aResponse().withStatus(200)));

        Connection.Response response = webClient.head("http://localhost:8089/test-page");

        assertNotNull(response);
        assertEquals(200, response.statusCode());
    }

    @Test
    void testFetchThrowsIOExceptionOnInvalidUrl() {
        assertThrows(IOException.class, () -> webClient.fetch("http://invalid.url"));
    }
}
