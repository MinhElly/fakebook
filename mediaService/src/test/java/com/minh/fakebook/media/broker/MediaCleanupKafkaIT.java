package com.minh.fakebook.media.broker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import com.minh.fakebook.media.IntegrationTest;
import com.minh.fakebook.media.domain.enumeration.MediaStatus;
import com.minh.fakebook.media.repository.MediaRepository;
import com.minh.fakebook.media.service.FileStorageService;
import com.minh.fakebook.media.domain.Media;
import com.minh.fakebook.media.domain.enumeration.MediaType;
import com.minh.fakebook.media.domain.enumeration.StorageProvider;
import java.io.IOException;
import java.time.Duration;
import java.util.*;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.kafka.KafkaContainer;

@IntegrationTest
@ActiveProfiles({"test", "kafka"})
class MediaCleanupKafkaIT {
    private static final KafkaContainer KAFKA = new KafkaContainer("apache/kafka-native:4.3.1");
    static { KAFKA.start(); }
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.stream.kafka.binder.brokers", KAFKA::getBootstrapServers);
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
    }
    @Autowired private MediaRepository repository;
    @MockitoBean private FileStorageService storage;

    @Test void storageFailureRetriesToDltAndSelectedReplayCompletes() throws Exception {
        var media = repository.saveAndFlush(new Media().ownerId(UUID.randomUUID()).fileName("proof.jpg").mediaType(MediaType.IMAGE).mimeType("image/jpeg").fileSize(1L).storageProvider(StorageProvider.CLOUDINARY).url("https://example.invalid/proof").status(MediaStatus.ACTIVE).createdAt(java.time.Instant.now()).storageKey("cleanup-kafka-" + UUID.randomUUID()));
        doThrow(new IOException("Storage outage")).when(storage).deleteFile(media.getStorageKey());
        String payload = "{\"mediaId\":\"" + media.getId() + "\",\"reason\":\"TEST\"}";
        Map<String, Object> consumerProperties = new HashMap<>();
        consumerProperties.put("bootstrap.servers", KAFKA.getBootstrapServers());
        consumerProperties.put("group.id", "cleanup-proof-" + UUID.randomUUID());
        consumerProperties.put("auto.offset.reset", "earliest");
        consumerProperties.put("key.deserializer", StringDeserializer.class);
        consumerProperties.put("value.deserializer", StringDeserializer.class);
        Map<String, Object> producerProperties = Map.of("bootstrap.servers", KAFKA.getBootstrapServers(),
            "key.serializer", StringSerializer.class, "value.serializer", StringSerializer.class);
        try (var consumer = new KafkaConsumer<String, String>(consumerProperties);
             var producer = new KafkaProducer<String, String>(producerProperties)) {
            consumer.subscribe(List.of("media-cleanup-dlt"));
            producer.send(new ProducerRecord<>("media-cleanup-topic", media.getId().toString(), payload)).get();
            String dltPayload = null;
            long deadline = System.nanoTime() + Duration.ofSeconds(45).toNanos();
            while (System.nanoTime() < deadline && dltPayload == null) {
                for (var record : consumer.poll(Duration.ofMillis(500))) if (record.value().contains(media.getId().toString())) dltPayload = record.value();
            }
            assertThat(dltPayload).isEqualTo(payload);
            verify(storage, atLeast(3)).deleteFile(media.getStorageKey());
            assertThat(repository.findById(media.getId()).orElseThrow().getStatus()).isEqualTo(MediaStatus.ACTIVE);
            doNothing().when(storage).deleteFile(media.getStorageKey());
            producer.send(new ProducerRecord<>("media-cleanup-topic", media.getId().toString(), dltPayload)).get();
            org.awaitility.Awaitility.await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
                assertThat(repository.findById(media.getId()).orElseThrow().getStatus()).isEqualTo(MediaStatus.DELETED));
            producer.send(new ProducerRecord<>("media-cleanup-topic", media.getId().toString(), payload)).get();
            verify(storage, timeout(10000).atLeast(5)).deleteFile(media.getStorageKey());
            assertThat(repository.findById(media.getId()).orElseThrow().getStatus()).isEqualTo(MediaStatus.DELETED);
        } finally { repository.deleteById(media.getId()); }
    }
}
