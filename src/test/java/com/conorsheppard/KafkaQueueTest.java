package com.conorsheppard;

import com.conorsheppard.queue.KafkaQueue;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class KafkaQueueTest {
    private KafkaProducer<String, String> mockProducer;
    private KafkaConsumer<String, String> mockConsumer;
    private KafkaQueue kafkaQueue;

    @BeforeEach
    void setUp() {
        kafkaQueue = spy(new KafkaQueue());
        mockProducer = mock(KafkaProducer.class);
        mockConsumer = mock(KafkaConsumer.class);

        // Override the producer and consumer in the spy instance
        doReturn(mockProducer).when(kafkaQueue).getProducer();
        doReturn(mockConsumer).when(kafkaQueue).getConsumer();
    }

    @Test
    void testEnqueue() {
        String url = "http://example.com";
        when(mockProducer.send(any(ProducerRecord.class))).thenReturn(CompletableFuture.completedFuture(null));

        kafkaQueue.enqueue(url);

        verify(mockProducer, times(1)).send(any(ProducerRecord.class));
    }

    @Test
    void testDequeue() {
        String expectedUrl = "http://example.com";
        var kafkaRecord = new ConsumerRecord<>(KafkaQueue.TOPIC, 0, 0L, "key", expectedUrl);
        var kafkaRecords = new ConsumerRecords<>(Collections.singletonMap(new TopicPartition(KafkaQueue.TOPIC, 0),
                Collections.singletonList(kafkaRecord)));
        when(mockConsumer.poll(Duration.ofMillis(100))).thenReturn(kafkaRecords);

        String actualUrl = kafkaQueue.dequeue();

        assertEquals(expectedUrl, actualUrl);
    }

    @Test
    void testDequeueWhenNoRecords() {
        when(mockConsumer.poll(Duration.ofMillis(100))).thenReturn(new ConsumerRecords<>(Collections.emptyMap()));

        String result = kafkaQueue.dequeue();

        assertNull(result);
    }

    @Test
    void testIsEmpty() {
        assertFalse(kafkaQueue.isEmpty());
    }

    @Test
    void testSize() {
        assertEquals(-1, kafkaQueue.size());
    }

    @Test
    void testContains() {
        assertFalse(kafkaQueue.contains("http://example.com"));
    }
}

