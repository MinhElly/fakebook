package com.minh.fakebook.feed.broker;

import java.time.Instant;
import java.util.UUID;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.minh.fakebook.feed.service.FeedFanoutService;
import com.minh.fakebook.feed.service.event.PostCreatedEvent;
import com.minh.fakebook.feed.service.event.PostUpdatedEvent;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component("processPostEvent")
public class PostEventListener implements Consumer<String> {
    private static final Logger LOG = LoggerFactory.getLogger(PostEventListener.class);

    private final ObjectMapper objectMapper;
    private final FeedFanoutService feedFanoutService;

    public PostEventListener(ObjectMapper objectMapper, FeedFanoutService feedFanoutService) {
        this.objectMapper = objectMapper;
        this.feedFanoutService = feedFanoutService;
    }

    @Override
    public void accept(String payload) {
        if (payload == null || payload.isBlank()) {
            throw new IllegalArgumentException("Post event payload must not be empty");
        }
        LOG.debug("Received post event payload: {}", payload);
        try {
            JsonNode root = objectMapper.readTree(payload);
            String eventType = "";
            if (root.has("eventType") && !root.get("eventType").isNull()) {
                eventType = root.get("eventType").asString();
            }
            JsonNode data = root.has("data") && !root.get("data").isNull() ? root.get("data") : root;
            if (eventType.isBlank() && data.has("eventType") && !data.get("eventType").isNull()) {
                eventType = data.get("eventType").asString();
            }

            if (eventType.isBlank()) {
                throw new IllegalArgumentException("Invalid post event payload: missing eventType");
            }

            if (!data.has("id") || data.get("id").isNull() || data.get("id").asString().isBlank()) {
                throw new IllegalArgumentException("Post event payload requires id");
            }
            UUID postId = UUID.fromString(data.get("id").asString());

            if ("POST_DELETED".equalsIgnoreCase(eventType)) {
                LOG.info("Processing post deletion event for postId: {}", postId);
                feedFanoutService.processPostDeleted(postId);
                return;
            }

            if (!"POST_CREATED".equalsIgnoreCase(eventType) && !"POST_UPDATED".equalsIgnoreCase(eventType)) {
                throw new IllegalArgumentException("Unsupported post eventType received: " + eventType);
            }

            if (!data.has("authorId") || data.get("authorId").isNull() || data.get("authorId").asString().isBlank()) {
                throw new IllegalArgumentException("Invalid post event payload: missing authorId for eventType " + eventType);
            }
            UUID authorId = UUID.fromString(data.get("authorId").asString());

            if (!data.has("visibility") || data.get("visibility").isNull() || data.get("visibility").asString().isBlank()) {
                throw new IllegalArgumentException("Invalid post event payload: visibility must not be null for postId " + postId);
            }
            String visibility = data.get("visibility").asString().toUpperCase();
            if (!java.util.Set.of("PUBLIC", "FRIENDS", "PRIVATE").contains(visibility)) {
                throw new IllegalArgumentException("Invalid post event visibility: " + visibility + " for postId " + postId);
            }

            String content = data.has("content") && !data.get("content").isNull() ? data.get("content").asString() : "";
            String status = data.has("status") && !data.get("status").isNull() ? data.get("status").asString() : "ACTIVE";

            if ("POST_UPDATED".equalsIgnoreCase(eventType)) {
                Instant updatedAt = data.has("updatedAt") && !data.get("updatedAt").isNull()
                        ? Instant.parse(data.get("updatedAt").asString())
                        : Instant.now();
                PostUpdatedEvent updatedEvent = new PostUpdatedEvent(postId, authorId,
                        content, visibility, status, updatedAt,
                        data.has("createdAt") && !data.get("createdAt").isNull() ? Instant.parse(data.get("createdAt").asString()) : null);
                feedFanoutService.processPostUpdated(updatedEvent);
            } else {
                Instant createdAt = data.has("createdAt") && !data.get("createdAt").isNull()
                        ? Instant.parse(data.get("createdAt").asString())
                        : Instant.now();
                PostCreatedEvent createdEvent = new PostCreatedEvent(postId, authorId,
                        content, visibility, status, createdAt);
                LOG.info("Processing PostCreatedEvent for postId: {} by authorId: {}",
                        postId, authorId);
                feedFanoutService.processPostCreated(createdEvent);
            }
        } catch (Exception e){
            LOG.error("Error parsing/processing post event payload '{}' : {}", payload, e.getMessage(), e);
            throw new RuntimeException("Failed to process post event payload: " + payload, e);
        }
    }
}
