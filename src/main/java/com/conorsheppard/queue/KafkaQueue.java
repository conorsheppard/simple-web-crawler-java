package com.conorsheppard.queue;

import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

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
        consumer.assign(Collections.singleton(new TopicPartition(TOPIC, 0)));
        // Poll once to allow the consumer to fetch the partition assignments
        consumer.poll(Duration.ofMillis(100));
    }

    @Override
    public void enqueue(String url) {
        this.getProducer().send(new ProducerRecord<>(TOPIC, url));
    }

    @Override
    public String dequeue() {
        var records = this.getConsumer().poll(Duration.ofMillis(500));
        if (records.isEmpty()) {
            return null;
        } else {
            var val = records.iterator().next().value();
            consumer.commitSync();
            return val;
        }
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public int size() {
        TopicPartition partition0 = new TopicPartition(TOPIC, 0);
        Map<TopicPartition, Long> endOffsets = this.getConsumer().endOffsets(Collections.singletonList(partition0));
        long currentOffset = this.getConsumer().position(partition0);
        long logEndOffset = endOffsets.get(partition0);
        long lag = logEndOffset - currentOffset;
        return (int) lag;
    }
}
