package com.minh.fakebook.post.service.event;

import java.util.UUID;

/**
 * Event published when a post is permanently deleted.
 */
public record PostDeletedEvent(
        UUID id) {
}