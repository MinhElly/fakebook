package com.minh.fakebook.user.service.dto.events;

import java.util.UUID;

public record MediaCleanupEvent(
    UUID mediaId,
    String reason
) {}
