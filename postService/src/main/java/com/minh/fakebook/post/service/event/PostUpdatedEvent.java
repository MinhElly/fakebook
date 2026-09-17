package com.minh.fakebook.post.service.event;

import com.minh.fakebook.post.domain.enumeration.PostStatus;
import com.minh.fakebook.post.domain.enumeration.PostVisibility;
import java.time.Instant;
import java.util.UUID;

/**
 * Event published when a post is updated (e.g., content or visibility changes).
 */

public record PostUpdatedEvent(
        UUID id,
        UUID authorId,
        String content,
        PostVisibility visibility,
        PostStatus status,
        Instant updatedAt) {
}
