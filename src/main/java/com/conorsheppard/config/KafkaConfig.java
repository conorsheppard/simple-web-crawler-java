package com.conorsheppard.config;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class KafkaConfig {

    public static Properties loadKafkaProducerProperties() {
        return loadKafkaProperties("kafka/" + System.getenv().getOrDefault("ENVIRONMENT", "prod")
                + "/kafka-producer.properties");
    }

    public static Properties loadKafkaConsumerProperties() {
        return loadKafkaProperties("kafka/" + System.getenv().getOrDefault("ENVIRONMENT", "prod")
                + "/kafka-consumer.properties");
    }

    @SneakyThrows
    private static Properties loadKafkaProperties(String fileName) {
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
