package com.minh.fakebook.post.service.event;

import com.minh.fakebook.post.domain.enumeration.PostStatus;
import com.minh.fakebook.post.domain.enumeration.PostVisibility;
import java.time.Instant;
import java.util.UUID;

/**
 * Event published when a new post is created.
 */

public record PostCreatedEvent(
    UUID id,
    UUID authorId,
    String content,
    PostVisibility visibility,
    PostStatus status,
    Instant createdAt) {
}
