package com.minh.fakebook.comment.service;
import com.minh.fakebook.comment.administration.*;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.jdbc.core.JdbcTemplate;
import com.minh.fakebook.comment.service.CommentSummaryCache;
import tools.jackson.databind.ObjectMapper;
@Component
public class ModerationProjectionConsumer {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final CommentSummaryCache cache;
    public ModerationProjectionConsumer(JdbcTemplate jdbc,ObjectMapper mapper,CommentSummaryCache cache) {this.jdbc=jdbc;this.mapper=mapper;this.cache=cache;}
    @KafkaListener(topics="content-moderation-events",groupId="commentservice-moderation")
    public void consume(String payload) {
        var event=mapper.readTree(payload);
        String post=java.util.UUID.fromString(event.path("postId").asString()).toString();
        if("POST".equals(event.path("kind").asString())) jdbc.update("delete from post_cache where id=?",post);
        cache.evict(java.util.UUID.fromString(post));
    }
}
