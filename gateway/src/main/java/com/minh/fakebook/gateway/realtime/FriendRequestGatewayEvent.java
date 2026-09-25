package com.minh.fakebook.gateway.realtime;

import java.time.Instant;
import java.util.UUID;

public record FriendRequestGatewayEvent(
    UUID eventId,
    String eventType,
    int eventVersion,
    Instant timestamp,
    Data data
) {
    public record Data(UUID requestId, UUID senderId, UUID receiverId) {}
}
