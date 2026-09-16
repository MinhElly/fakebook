package com.minh.fakebook.feed.broker;

import com.minh.fakebook.feed.service.FeedFanoutService;
import com.minh.fakebook.feed.service.dto.event.PostCreatedEvent;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Spring Cloud Stream Consumer for post-events topic ("processPostEvent-in-0").
 */
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
        LOG.debug("Received post event payload: {}", payload);
        try {
            JsonNode root = objectMapper.readTree(payload);
            if (!root.has("id") || root.get("id").isNull()) {
                LOG.warn("Ignoring invalid post event payload without 'id': {}", payload);
                return;
            }

            UUID postId = UUID.fromString(root.get("id").asText());

            // Deletion event check (authorId missing or null)
            if (!root.has("authorId") || root.get("authorId").isNull()) {
                LOG.info("Processing post deletion event for postId: {}", postId);
                feedFanoutService.processPostDeleted(postId);
                return;
            }

            UUID authorId = UUID.fromString(root.get("authorId").asText());
            String visibility = root.has("visibility") && !root.get("visibility").isNull()
                ? root.get("visibility").asText()
                : "PUBLIC";
            Instant createdAt = root.has("createdAt") && !root.get("createdAt").isNull()
                ? Instant.parse(root.get("createdAt").asText())
                : Instant.now();

            PostCreatedEvent event = new PostCreatedEvent(postId, authorId, visibility, createdAt);
            LOG.info("Processing PostCreatedEvent for postId: {} by authorId: {}", postId, authorId);
            feedFanoutService.processPostCreated(event);
        } catch (Exception e) {
            LOG.error("Error parsing/processing post event payload '{}': {}", payload, e.getMessage(), e);
        }
    }
}
