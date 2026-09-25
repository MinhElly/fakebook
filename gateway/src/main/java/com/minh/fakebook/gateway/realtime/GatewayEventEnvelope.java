package com.minh.fakebook.gateway.realtime;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record GatewayEventEnvelope(
    UUID eventId,
    String eventType,
    int eventVersion,
    Instant timestamp,
    Map<String, Object> data
) {}
