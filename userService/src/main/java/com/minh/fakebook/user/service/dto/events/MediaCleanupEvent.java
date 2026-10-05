package com.minh.fakebook.user.service.dto.events;

import io.namastack.outbox.annotation.OutboxEvent;
import java.util.UUID;

@OutboxEvent
public record MediaCleanupEvent(
    UUID mediaId,
    String reason
) {}
