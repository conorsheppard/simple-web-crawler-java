package com.conorsheppard.web;

import org.jsoup.Connection.Response;
import org.jsoup.nodes.Document;

import java.io.IOException;

public interface WebClient {
    Document fetch(String url) throws IOException;
    Response head(String url) throws IOException;
}
