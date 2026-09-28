package com.minh.fakebook.feed.service.event;

import java.time.Instant;
import java.util.UUID;

public record FriendshipUpdatedEvent(UUID eventId, String eventType, int eventVersion, Instant timestamp, Data data) {
    public record Data(UUID userId, UUID friendId) {}
}
