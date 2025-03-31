package com.conorsheppard.queue;

import com.conorsheppard.config.KafkaConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class KafkaPropertiesTest {
    private Properties mockProducerProps;
    private Properties mockConsumerProps;

    @BeforeEach
    void setUp() {
        mockProducerProps = new Properties();
        mockProducerProps.put("bootstrap.servers", "localhost:9092");
        mockProducerProps.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        mockProducerProps.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        mockConsumerProps = new Properties();
        mockConsumerProps.put("bootstrap.servers", "localhost:9092");
        mockConsumerProps.put("group.id", "test-group");
        mockConsumerProps.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        mockConsumerProps.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        mockConsumerProps.put("auto.offset.reset", "earliest");
    }

    @Test
    void testKafkaQueueConstructorLoadsProperties() {
        try (MockedStatic<KafkaConfig> mockedKafkaConfig = Mockito.mockStatic(KafkaConfig.class)) {
            mockedKafkaConfig.when(KafkaConfig::loadKafkaProducerProperties).thenReturn(mockProducerProps);
            mockedKafkaConfig.when(KafkaConfig::loadKafkaConsumerProperties).thenReturn(mockConsumerProps);
            KafkaQueue kafkaQueue = new KafkaQueue();
            assertNotNull(kafkaQueue.getProducer());
            assertNotNull(kafkaQueue.getConsumer());
        }
    }
}
