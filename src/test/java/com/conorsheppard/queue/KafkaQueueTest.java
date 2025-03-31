package com.conorsheppard.queue;

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
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class KafkaQueueTest {
    private KafkaProducer<String, String> mockProducer;
    private KafkaConsumer<String, String> mockConsumer;
    private KafkaQueue kafkaQueue;
    private static final String TOPIC = "web-crawler-urls";

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
        when(mockConsumer.poll(Duration.ofMillis(500))).thenReturn(kafkaRecords);

        String actualUrl = kafkaQueue.dequeue();

        assertEquals(expectedUrl, actualUrl);
    }

    @Test
    void testDequeueWhenNoRecords() {
        when(mockConsumer.poll(Duration.ofMillis(500))).thenReturn(new ConsumerRecords<>(Collections.emptyMap()));
        String result = kafkaQueue.dequeue();
        assertNull(result);
    }

    @Test
    void testIsEmptyWhenSizeIsZero() {
        // Prepare mocks for endOffsets and position
        TopicPartition partition = new TopicPartition(TOPIC, 0);

        // Define mock behavior for the KafkaConsumer methods
        Map<TopicPartition, Long> endOffsets = Collections.singletonMap(partition, 100L);
        when(mockConsumer.endOffsets(Collections.singletonList(partition))).thenReturn(endOffsets);
        when(mockConsumer.position(partition)).thenReturn(100L);  // Position == endOffset means size() should be 0

        // Call isEmpty() and verify the result
        assertTrue(kafkaQueue.isEmpty(), "The isEmpty() method should return true when size() is 0");

        // Verify that KafkaConsumer methods were called
        verify(mockConsumer).endOffsets(Collections.singletonList(partition));
        verify(mockConsumer).position(partition);
    }

    @Test
    void testIsEmptyWhenSizeIsNotZero() {
        // Prepare mocks for endOffsets and position
        TopicPartition partition = new TopicPartition(TOPIC, 0);

        // Define mock behavior for the KafkaConsumer methods
        Map<TopicPartition, Long> endOffsets = Collections.singletonMap(partition, 100L);
        when(mockConsumer.endOffsets(Collections.singletonList(partition))).thenReturn(endOffsets);
        when(mockConsumer.position(partition)).thenReturn(90L);  // Position < endOffset means size() should be > 0

        // Call isEmpty() and verify the result
        assertFalse(kafkaQueue.isEmpty(), "The isEmpty() method should return false when size() is not 0");

        // Verify that KafkaConsumer methods were called
        verify(mockConsumer).endOffsets(Collections.singletonList(partition));
        verify(mockConsumer).position(partition);
    }

    @Test
    void testSize() {
        // Prepare mocks for endOffsets and position
        TopicPartition partition0 = new TopicPartition(TOPIC, 0);

        // Define mock behavior for the KafkaConsumer methods
        Map<TopicPartition, Long> endOffsets = Collections.singletonMap(partition0, 100L);
        when(mockConsumer.endOffsets(Collections.singletonList(partition0))).thenReturn(endOffsets);
        when(mockConsumer.position(partition0)).thenReturn(90L);

        // Call the method you want to test
        int size = kafkaQueue.size();

        // Validate that the method returns the correct value
        // lag = logEndOffset - currentOffset = 100 - 90 = 10
        assertEquals(10, size);

        // Verify that KafkaConsumer methods were called
        verify(mockConsumer).endOffsets(Collections.singletonList(partition0));
        verify(mockConsumer).position(partition0);
    }
}

