package com.minh.fakebook.gateway.config;

import java.time.Duration;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.kafka.KafkaContainer;

/**
 * Sample Kafka Test Container configuration for integration tests. This will start a Kafka container before the tests and stop it afterwards.
 * To use it, add KafkaTestContainer.class to the classes of @SpringBootTest on your test class.
 */
@TestConfiguration(proxyBeanMethods = false)
public class KafkaTestContainer {

    private static final KafkaContainer KAFKA_CONTAINER = new KafkaContainer("apache/kafka-native:4.3.1")
        .withStartupTimeout(Duration.ofMinutes(2))
        .withStartupAttempts(3)
        .withEnv("KAFKA_LISTENERS", "PLAINTEXT://:9092,BROKER://:9093,CONTROLLER://:9094")
        .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(KafkaTestContainer.class)));

    @Bean
    @ServiceConnection
    KafkaContainer kafkaContainer() {
        return KAFKA_CONTAINER;
    }
}
