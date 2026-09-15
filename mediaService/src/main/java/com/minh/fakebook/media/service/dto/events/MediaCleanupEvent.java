package com.minh.fakebook.media.service.dto.events;

import java.util.UUID;

public record MediaCleanupEvent(
    UUID mediaId,
    String reason) {
}
