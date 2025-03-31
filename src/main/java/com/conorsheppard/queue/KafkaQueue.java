package com.conorsheppard.queue;

import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.TopicPartition;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutionException;

import static com.conorsheppard.config.KafkaConfig.*;

@Slf4j
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
//        consumer.subscribe(Collections.singleton(TOPIC));
        consumer.assign(Collections.singleton(new TopicPartition(TOPIC, 0)));
        // Poll once to allow the consumer to fetch the partition assignments
        consumer.poll(Duration.ofMillis(100));
        // Check if partitions are assigned
        Set<TopicPartition> partitions = consumer.assignment();
        log.debug("Partitions: {}", partitions);
    }

    @Override
    public void enqueue(String url) {
        this.getProducer().send(new ProducerRecord<>(TOPIC, url));
    }

    @Override
    public String dequeue() {
        var records = this.getConsumer().poll(Duration.ofMillis(500));
        if (records.isEmpty()) {
            log.debug("records is empty");
            return null;
        } else {
            var val = records.iterator().next().value();
            consumer.commitSync();
            log.debug("dequeued URL: {}", val);
            return val;
        }
//        return records.isEmpty() ? null : records.iterator().next().value();
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
