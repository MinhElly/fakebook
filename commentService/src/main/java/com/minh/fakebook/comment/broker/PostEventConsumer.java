package com.minh.fakebook.comment.broker;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.messaging.Message;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.Acknowledgment;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.function.Consumer;

/**
 * Kafka consumer for post events.
 */
@Component("processPostEvent")
public class PostEventConsumer implements Consumer<Message<String>> {

    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;

    public PostEventConsumer(ObjectMapper objectMapper, JdbcTemplate jdbcTemplate) {
        this.objectMapper = objectMapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void accept(Message<String> message) {
        String payload = message.getPayload();
        try {
            JsonNode jsonNode = objectMapper.readTree(payload);
            String eventType = jsonNode.has("eventType") && !jsonNode.get("eventType").isNull() ? jsonNode.get("eventType").asText() : "";
            
            if ("POST_DELETED".equalsIgnoreCase(eventType) || !jsonNode.has("authorId") || jsonNode.get("authorId").isNull()) {
                String postId = jsonNode.get("id").asText();
                jdbcTemplate.update("DELETE FROM post_cache WHERE id = ?", postId);
            } else {
                String postId = jsonNode.get("id").asText();
                String authorId = jsonNode.get("authorId").asText();
                String status = jsonNode.has("status") && !jsonNode.get("status").isNull() ? jsonNode.get("status").asText() : "ACTIVE";
                String visibility = jsonNode.has("visibility") && !jsonNode.get("visibility").isNull() ? jsonNode.get("visibility").asText() : "PUBLIC";

                String sql = "INSERT INTO post_cache (id, author_id, status, visibility) VALUES (?, ?, ?, ?) " + "ON DUPLICATE KEY UPDATE author_id = ?, status = ?, visibility = ?";jdbcTemplate.update(sql, postId, authorId, status, visibility, authorId, status, visibility);
            }
            
            Acknowledgment acknowledgment = message.getHeaders().get(KafkaHeaders.ACKNOWLEDGMENT, Acknowledgment.class);
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }
        } catch (Exception e) {
            System.err.println("Kafka process error: " + e.getMessage());
            throw new RuntimeException("Kafka process failed, triggering retry or DLT", e);
        }
    }
}
