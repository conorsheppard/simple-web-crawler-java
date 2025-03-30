package com.conorsheppard.queue;

import lombok.Data;
import lombok.SneakyThrows;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

import static com.conorsheppard.config.KafkaConfig.*;

@Data
public class KafkaQueue implements UrlQueue {
    public static final String TOPIC = "web-crawler-urls";
    private final KafkaProducer<String, String> producer;
    private final KafkaConsumer<String, String> consumer;

    @SneakyThrows
    public KafkaQueue() {
        Properties producerProps = loadKafkaProducerProperties();
        Properties consumerProps = loadKafkaConsumerProperties();
        producer = new KafkaProducer<>(producerProps);
        consumer = new KafkaConsumer<>(consumerProps);
        consumer.subscribe(Collections.singleton(TOPIC));
    }

    @Override
    public void enqueue(String url) {
        this.getProducer().send(new ProducerRecord<>(TOPIC, url));
    }

    @Override
    public String dequeue() {
        var records = this.getConsumer().poll(Duration.ofMillis(100));
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
