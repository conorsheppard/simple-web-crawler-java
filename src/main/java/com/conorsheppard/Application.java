package com.conorsheppard;

import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

@Slf4j
public class Application {
    public static void main(String[] args) {
        Arrays.stream(args).forEach(arg -> log.info("arg: {}", arg));
        if (args.length != 1) {
            log.info("Usage: Application <start-url>");
            return;
        }
        new SimpleWebCrawler(args[0]).crawl();
    }
}
