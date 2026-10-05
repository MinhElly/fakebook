package com.minh.fakebook.comment.service.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Event published when a comment is created, updated, or deleted.
 */
public record CommentChangedEvent(
        /** ID of the Post where the comment belongs to */
        UUID postId,
        /** The type of action: CREATED, UPDATED, DELETED */
        String action,
        /** Timestamp of the event */
        Instant occurredAt) {
}
