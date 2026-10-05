package com.minh.fakebook.user.service.event;

import java.time.Instant;
import java.util.UUID;
import io.namastack.outbox.annotation.OutboxEvent;

@OutboxEvent
public record FriendshipUpdatedEvent(
        UUID eventId,
        String eventType,
        int eventVersion,
        Instant timestamp,
        Data data) {
    public static FriendshipUpdatedEvent created(UUID userId, UUID friendId) {
        return new FriendshipUpdatedEvent(UUID.randomUUID(), "FRIENDSHIP_CREATED", 1, Instant.now(), new Data(userId, friendId));
    }
    public static FriendshipUpdatedEvent deleted(UUID userId, UUID friendId) {
        return new FriendshipUpdatedEvent(UUID.randomUUID(), "FRIENDSHIP_DELETED", 1, Instant.now(), new Data(userId, friendId));
    }
    public record Data(UUID userId, UUID friendId) {}
}
