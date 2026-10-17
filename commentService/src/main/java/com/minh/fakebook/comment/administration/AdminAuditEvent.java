package com.minh.fakebook.comment.administration;

import java.time.Instant;
import io.namastack.outbox.annotation.OutboxEvent;
@OutboxEvent
public record AdminAuditEvent(String eventId, String actor, String target, String action, String reason,
    Instant occurredAt, String beforeState, String afterState, String requestId) {}
