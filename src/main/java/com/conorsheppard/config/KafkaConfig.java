package com.conorsheppard.config;

import lombok.SneakyThrows;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class KafkaConfig {
    @SneakyThrows
    public static Properties loadKafkaProperties(String fileName) {
        Properties properties = new Properties();
        // Load the properties file from the classpath
        try (InputStream inputStream = KafkaConfig.class.getClassLoader().getResourceAsStream(fileName)) {
            if (inputStream == null) {
                throw new IOException("Property file not found in the classpath: " + fileName);
            }
            properties.load(inputStream);
        }
        return properties;
    }
}
