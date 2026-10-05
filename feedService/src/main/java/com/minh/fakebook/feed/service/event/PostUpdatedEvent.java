package com.minh.fakebook.feed.service.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Event received when a post is updated.
 */
public record PostUpdatedEvent(
    UUID id,
    UUID authorId,
    String content,
    String visibility,
    String status,
    Instant updatedAt
) {}
