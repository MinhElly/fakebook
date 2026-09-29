package com.minh.fakebook.comment.service.event;

import java.time.Instant;
import java.util.UUID;
import io.namastack.outbox.annotation.OutboxEvent;

@OutboxEvent
public record EventEnvelope<T>(
        UUID eventId,
        String eventType,
        int eventVersion,
        Instant timestamp,
        T data) {
}
