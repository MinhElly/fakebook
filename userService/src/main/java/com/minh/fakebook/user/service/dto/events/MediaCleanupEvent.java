package com.minh.fakebook.user.service.dto.events;

import io.namastack.outbox.annotation.OutboxEvent;
import java.util.UUID;

@Deprecated(forRemoval = false)
/** Compatibility payload for persisted outbox records with the old class name. */
public record MediaCleanupEvent(
    UUID mediaId,
    String reason
) {}
