package com.minh.fakebook.user.service.dto.events;

import java.time.Instant;
import java.util.UUID;

public record FriendshipUpdatedEvent(
        UUID userId,
        UUID friendId,
        String action,
        Instant timestamp) {
}
