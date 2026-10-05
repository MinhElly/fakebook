package com.minh.fakebook.user.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.micrometer.tracing.propagation.Propagator;
import io.namastack.outbox.handler.OutboxRecordMetadata;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class OutboxKafkaRoutingConfigurationTest {

    private final OutboxKafkaRoutingConfiguration configuration = new OutboxKafkaRoutingConfiguration();

    @Test
    void shouldRouteFriendshipEventsByDefault() {
        var routing = configuration.kafkaOutboxRouting(Optional.empty());

        assertThat(routing.resolveTopic(new Object(), metadata(Map.of()))).isEqualTo("friendship-events");
    }

    @Test
    void shouldRouteFriendRequestEvents() {
        var routing = configuration.kafkaOutboxRouting(Optional.empty());

        assertThat(routing.resolveTopic(new Object(), metadata(Map.of("destination", "friend-request-events"))))
            .isEqualTo("friend-request-events");
    }

    @Test
    void shouldRouteMediaCleanupAndForwardOnlyTracingHeaders() {
        Propagator propagator = mock(Propagator.class);
        when(propagator.fields()).thenReturn(List.of("traceparent"));
        var routing = configuration.kafkaOutboxRouting(Optional.of(propagator));
        var metadata = metadata(Map.of(
            "destination", "media-cleanup-topic",
            "traceparent", "00-trace-span-01",
            "tenantId", "must-not-leak"
        ));

        assertThat(routing.resolveTopic(new Object(), metadata)).isEqualTo("media-cleanup-topic");
        assertThat(routing.buildHeaders(new Object(), metadata))
            .containsOnly(Map.entry("traceparent", "00-trace-span-01"));
    }

    private OutboxRecordMetadata metadata(Map<String, String> context) {
        return new OutboxRecordMetadata("key", "handler", Instant.parse("2026-09-30T00:00:00Z"), context);
    }
}
