package com.minh.fakebook.post.service.event;

import io.namastack.outbox.annotation.OutboxEvent;
import java.util.UUID;

@OutboxEvent
public record MediaCleanupEvent(
    UUID mediaId,
    String reason
) {}
