package com.minh.fakebook.feed.administration;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.ObjectMapper;
@Component
public class ModerationProjectionConsumer {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    public ModerationProjectionConsumer(JdbcTemplate jdbc,ObjectMapper mapper) {this.jdbc=jdbc;this.mapper=mapper;}
    @KafkaListener(topics="content-moderation-events",groupId="feedservice-moderation")
    public void consume(String payload) {
        var event=mapper.readTree(payload);
        if(!"POST".equals(event.path("kind").asString())) return;
        // These rows are disposable; authoritative candidates rebuild the feed on subsequent reads.
        jdbc.update("delete from feed_items where post_id=?",java.util.UUID.fromString(event.path("postId").asString()).toString());
    }
}
