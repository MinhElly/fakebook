package com.minh.fakebook.user.service.event;

import java.time.Instant;
import java.util.UUID;
import io.namastack.outbox.annotation.OutboxEvent;

@OutboxEvent
public record FriendRequestCreatedEvent(
    UUID eventId,
    String eventType,
    int eventVersion,
    Instant timestamp,
    Data data
) {
    public static FriendRequestCreatedEvent create(UUID requestId, UUID senderId, UUID receiverId) {
        return new FriendRequestCreatedEvent(
            UUID.randomUUID(),
            "FRIEND_REQUEST_CREATED",
            1,
            Instant.now(),
            new Data(requestId, senderId, receiverId)
        );
    }

    public record Data(UUID requestId, UUID senderId, UUID receiverId) {}
}
