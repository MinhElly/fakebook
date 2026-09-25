package com.minh.fakebook.gateway.realtime;

import java.time.Instant;
import java.util.UUID;

public record RealtimeEvent(UUID eventId, String eventType, UUID postId, Instant occurredAt) {
}
