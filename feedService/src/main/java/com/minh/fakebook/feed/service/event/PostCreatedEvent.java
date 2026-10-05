package com.minh.fakebook.feed.service.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Event received when a new post is created.
 */
public record PostCreatedEvent(
    UUID id,
    UUID authorId,
    String content,
    String visibility,
    String status,
    Instant createdAt
) {}
