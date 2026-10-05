package com.minh.fakebook.feed.service.event;

import java.util.UUID;

/**
 * Event received when a post is deleted.
 */
public record PostDeletedEvent(
    UUID id
) {}
