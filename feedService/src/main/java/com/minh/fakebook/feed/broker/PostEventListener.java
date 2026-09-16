package com.minh.fakebook.feed.broker;

import java.time.Instant;
import java.util.UUID;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minh.fakebook.feed.service.FeedFanoutService;
import com.minh.fakebook.feed.service.event.PostCreatedEvent;

public class PostEventListener implements Consumer<String>{
    private static final Logger LOG = LoggerFactory.getLogger(PostEventListener.class);
    
    private final ObjectMapper objectMapper;
    private final FeedFanoutService feedFanoutService;

    public PostEventListener(ObjectMapper objectMapper, FeedFanoutService feedFanoutService){
        this.objectMapper = objectMapper;
        this. feedFanoutService = feedFanoutService;
    }

    @Override 
    public void accept(String payload) {
        if(payload == null  || payload.isBlank()){
            LOG.warn("Ignoring empty or null post even payload");
            return;
        }
        LOG.debug("Received post event payload: {}", payload);
        try{
            JsonNode root = objectMapper.readTree(payload);
            if(!root.has("id") || root.get("id").isNull()){
                LOG.warn("Ignoring invalid post event payload without 'id': {}", payload );
                return ;
            }
            UUID postId = UUID.fromString(root.get("id").asText());
            if(!root.has("authorId") || root.get("authorId").isNull()){
                LOG.info("Processing post deletion event for postId: {}", postId);
                feedFanoutService.processPostDeleted(postId);
                return ;
            }
            UUID authorId = UUID.fromString(root.get("authorId").asText());
            String content = root.has("content") && !root.get("content").isNull() ? root.get("content").asText() : "";
            String visibility = root.has("visibility") && !root.get("visibility").isNull() ? root.get("visibility").asText() : "PUBLIC";
            String status = root.has("status") && !root.get("status").isNull() ? root.get("status").asText() : "ACTIVE";
            Instant createdAt = root.has("createdAt") && !root.get("createdAt").isNull() ? Instant.parse(root.get("createdAt").asText()) : Instant.now();
            PostCreatedEvent event = new PostCreatedEvent(postId, authorId, content, visibility, status, createdAt);
            LOG.info("Processing PostCreatedEvent for postId: {} by authorId: {}", postId, authorId);
            feedFanoutService.processPostCreated(event);
        } catch (Exception e){
            LOG.error("Error parsing/processing post event payload '{}' : {}", payload, e.getMessage(), e);
        }
    }

}
