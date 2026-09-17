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
            try {
                JsonNode rootNode = objectMapper.readTree(message.getPayload());
                String eventType = rootNode.has("eventType") ? rootNode.get("eventType").asString() : "";
                JsonNode data = rootNode.has("data") ? rootNode.get("data") : rootNode;

                if ("POST_DELETED".equals(eventType) || !data.has("authorId") || data.get("authorId").isNull()) {
                    String postId = data.get("id").asString();
                    jdbcTemplate.update("DELETE FROM post_cache WHERE id = ?", postId);
                } else {
                    String postId = data.get("id").asString();
                    String authorId = data.get("authorId").asString();
                    String status = data.has("status") && !data.get("status").isNull() ?data.get("status").asString() : "ACTIVE";
                    String visibility = data.has("visibility") && !data.get("visibility").isNull() ? data.get("visibility").asString() : "PUBLIC";

                    String sql = "INSERT INTO post_cache (id, author_id, status, visibility) VALUES (?, ?, ?, ?) " + "ON DUPLICATE KEY UPDATE author_id = ?, status = ?, visibility = ?";
                    jdbcTemplate.update(sql, postId, authorId, status, visibility, authorId, status, visibility);
                }
                Acknowledgment ack = message.getHeaders().get(KafkaHeaders.ACKNOWLEDGMENT, Acknowledgment.class);
                if (ack != null) ack.acknowledge();
            } catch (Exception e) {
                System.err.println("Kafka process error: " + e.getMessage());
                throw new RuntimeException("Kafka process failed, triggering retry or DLT",
  e);
            }
        }
    }