package com.conorsheppard.web;

import org.jsoup.Connection;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;

public class JSoupWebClient implements WebClient {
    @Override
    public Document fetch(String url) throws IOException {
        return Jsoup.connect(url).timeout(5000).get();
    }

    @Override
    public Response head(String url) throws IOException {
        return Jsoup.connect(url).method(Connection.Method.HEAD).execute();
    }
}
