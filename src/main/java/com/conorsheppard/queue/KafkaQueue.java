package com.conorsheppard.queue;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

public class KafkaQueue implements UrlQueue {
    private static final String TOPIC = "web-crawler-urls";
    private final KafkaProducer<String, String> producer;
    private final KafkaConsumer<String, String> consumer;

    public KafkaQueue(String bootstrapServers) {
        // Kafka Producer Properties
        Properties producerProps = new Properties();
        producerProps.put("bootstrap.servers", bootstrapServers);
        producerProps.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        producerProps.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        producer = new KafkaProducer<>(producerProps);

        // Kafka Consumer Properties
        Properties consumerProps = new Properties();
        consumerProps.put("bootstrap.servers", bootstrapServers);
        consumerProps.put("group.id", "crawler-group");
        consumerProps.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        consumerProps.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        consumerProps.put("auto.offset.reset", "earliest");
        consumer = new KafkaConsumer<>(consumerProps);
        consumer.subscribe(Collections.singleton(TOPIC));
    }

    @Override
    public void enqueue(String url) {
        producer.send(new ProducerRecord<>(TOPIC, url));
    }

    @Override
    public String dequeue() {
        var records = consumer.poll(Duration.ofMillis(100));
        return records.isEmpty() ? null : records.iterator().next().value();
    }

    @Override
    public boolean isEmpty() {
        // Kafka is always "ready" to fetch
        return false;
    }

    @Override
    public int size() {
        // Kafka does not expose queue size directly
        return -1;
    }

    @Override
    public boolean contains(String url) {
        return false;
    }
}
