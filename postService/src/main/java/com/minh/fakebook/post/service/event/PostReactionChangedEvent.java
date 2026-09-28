package com.minh.fakebook.post.service.event;

import java.time.Instant;
import java.util.UUID;

public record PostReactionChangedEvent(UUID postId, String action, Instant occurredAt) {
}
