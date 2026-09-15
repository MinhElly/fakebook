package com.minh.fakebook.post.service.event;

import java.util.UUID;

public record MediaCleanupEvent(
    UUID mediaId,
    String reason
) {}
