package com.minh.fakebook.comment.broker;

import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component("processPostEvent")
public class PostEventConsumer implements Consumer<Message<String>> {

    private static final Logger LOG = LoggerFactory.getLogger(PostEventConsumer.class);
    private static final Set<String> VALID_VISIBILITIES = Set.of("PUBLIC", "FRIENDS", "PRIVATE");

    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;

    public PostEventConsumer(ObjectMapper objectMapper, JdbcTemplate jdbcTemplate) {
        this.objectMapper = objectMapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void accept(Message<String> message) {
        String payload = message != null ? message.getPayload() : null;
        if (payload == null || payload.isBlank()) {
            throw new IllegalArgumentException("Post event payload must not be empty");
        }
        try {
            JsonNode rootNode = objectMapper.readTree(payload);
            String eventType = "";
            if (rootNode.has("eventType") && !rootNode.get("eventType").isNull()) {
                eventType = rootNode.get("eventType").asString();
            }
            JsonNode data = rootNode.has("data") && !rootNode.get("data").isNull() ? rootNode.get("data") : rootNode;
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
                jdbcTemplate.update("DELETE FROM post_cache WHERE id = ?", postId.toString());
            } else if ("POST_CREATED".equalsIgnoreCase(eventType) || "POST_UPDATED".equalsIgnoreCase(eventType)) {
                if (!data.has("authorId") || data.get("authorId").isNull() || data.get("authorId").asString().isBlank()) {
                    throw new IllegalArgumentException("Invalid post event payload: missing authorId for eventType " + eventType);
                }
                UUID authorId = UUID.fromString(data.get("authorId").asString());

                if (!data.has("visibility") || data.get("visibility").isNull() || data.get("visibility").asString().isBlank()) {
                    throw new IllegalArgumentException("Invalid post event payload: visibility must not be null for postId " + postId);
                }
                String visibility = data.get("visibility").asString().toUpperCase();
                if (!VALID_VISIBILITIES.contains(visibility)) {
                    throw new IllegalArgumentException("Invalid post event visibility: " + visibility + " for postId " + postId);
                }

                String status = data.has("status") && !data.get("status").isNull() ? data.get("status").asString() : "ACTIVE";

                String sql = "INSERT INTO post_cache (id, author_id, status, visibility) VALUES (?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE author_id = ?, status = ?, visibility = ?";
                jdbcTemplate.update(sql, postId.toString(), authorId.toString(), status, visibility, authorId.toString(), status, visibility);
                LOG.info("Successfully updated post_cache for postId: {}, authorId: {}, status: {}, visibility: {}", postId, authorId, status, visibility);
            } else {
                throw new IllegalArgumentException("Unsupported post eventType received: " + eventType);
            }

            Acknowledgment ack = message.getHeaders() != null ? message.getHeaders().get(KafkaHeaders.ACKNOWLEDGMENT, Acknowledgment.class) : null;
            if (ack != null) {
                ack.acknowledge();
            }
        } catch (Exception e) {
            LOG.error("Kafka process error for payload '{}': {}", payload, e.getMessage(), e);
            throw new RuntimeException("Kafka process failed, triggering retry or DLT", e);
        }
    }
}