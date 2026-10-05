package com.minh.fakebook.post.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.micrometer.tracing.propagation.Propagator;
import io.namastack.outbox.handler.OutboxRecordMetadata;

class OutboxKafkaRoutingConfigurationTest {

    private final OutboxKafkaRoutingConfiguration configuration = new OutboxKafkaRoutingConfiguration();

    @Test
    void shouldForwardOnlyConfiguredPropagationHeadersOnDefaultRoute() {
        Propagator propagator = mock(Propagator.class);
        when(propagator.fields()).thenReturn(List.of("traceparent", "b3"));
        var routing = configuration.kafkaOutboxRouting(Optional.of(propagator));
        var metadata = metadata(Map.of(
            "traceparent", "00-trace-span-01",
            "b3", "trace-span-1",
            "tenantId", "must-not-leak"
        ));

        assertThat(routing.resolveTopic(new Object(), metadata)).isEqualTo("post-events");
        assertThat(routing.buildHeaders(new Object(), metadata))
            .containsOnly(
                Map.entry("traceparent", "00-trace-span-01"),
                Map.entry("b3", "trace-span-1")
            );
    }

    @Test
    void shouldApplyTheSamePropagationAllowlistOnReactionRoute() {
        Propagator propagator = mock(Propagator.class);
        when(propagator.fields()).thenReturn(List.of("traceparent"));
        var routing = configuration.kafkaOutboxRouting(Optional.of(propagator));
        var metadata = metadata(Map.of(
            "destination", "post-reaction-events",
            "traceparent", "00-trace-span-01",
            "correlationId", "must-not-leak"
        ));

        assertThat(routing.resolveTopic(new Object(), metadata)).isEqualTo("post-reaction-events");
        assertThat(routing.buildHeaders(new Object(), metadata))
            .containsOnly(Map.entry("traceparent", "00-trace-span-01"));
    }

    @Test
    void shouldRouteMediaCleanupWithPropagationHeaders() {
        Propagator propagator = mock(Propagator.class);
        when(propagator.fields()).thenReturn(List.of("traceparent"));
        var routing = configuration.kafkaOutboxRouting(Optional.of(propagator));
        var metadata = metadata(Map.of(
            "destination", "media-cleanup-topic",
            "traceparent", "00-trace-span-01"
        ));

        assertThat(routing.resolveTopic(new Object(), metadata)).isEqualTo("media-cleanup-topic");
        assertThat(routing.buildHeaders(new Object(), metadata))
            .containsOnly(Map.entry("traceparent", "00-trace-span-01"));
    }

    @Test
    void shouldForwardNoContextWhenTracingPropagatorIsUnavailable() {
        var routing = configuration.kafkaOutboxRouting(Optional.empty());
        var metadata = metadata(Map.of("traceparent", "00-trace-span-01"));

        assertThat(routing.buildHeaders(new Object(), metadata)).isEmpty();
    }

    private OutboxRecordMetadata metadata(Map<String, String> context) {
        return new OutboxRecordMetadata("key", "handler", Instant.parse("2026-09-29T00:00:00Z"), context);
    }
}
