package com.minh.fakebook.post.service.event;

import java.time.Instant;
import java.util.UUID;
import io.namastack.outbox.annotation.OutboxEvent;

/**
 * Standard Envelope for all Kafka Events in the system.
 */
@OutboxEvent
public record EventEnvelope<T>(
        UUID eventId,
        String eventType,
        int eventVersion,
        Instant timestamp,
        T data) {
}