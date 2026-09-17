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
public class PostEventListener implements Consumer<String>{
    private static final Logger LOG = LoggerFactory.getLogger(PostEventListener.class);
    
    private final ObjectMapper objectMapper;
    private final FeedFanoutService feedFanoutService;

    public PostEventListener(ObjectMapper objectMapper, FeedFanoutService feedFanoutService){
        this.objectMapper = objectMapper;
        this.feedFanoutService = feedFanoutService;
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
            String eventType = root.has("eventType") && !root.get("eventType").isNull() ? root.get("eventType").asText() : "";

            
            if("POST_DELETED".equalsIgnoreCase(eventType) || !root.has("authorId") || root.get("authorId").isNull()){
                LOG.info("Processing post deletion event for postId: {}", postId);
                feedFanoutService.processPostDeleted(postId);
                return ;
            }
            UUID authorId = UUID.fromString(root.get("authorId").asText());
            String content = root.has("content") && !root.get("content").isNull() ? root.get("content").asText() : "";
            String visibility = root.has("visibility") && !root.get("visibility").isNull() ? root.get("visibility").asText() : "PUBLIC";
            String status = root.has("status") && !root.get("status").isNull() ? root.get("status").asText() : "ACTIVE";
            
            if("POST_UPDATED".equalsIgnoreCase(eventType) || root.has("updatedAt")){
                Instant updatedAt = root.has("updatedAt") && !root.get("updatedAt").isNull() ? Instant.parse(root.get("updatedAt").asText()) : Instant.now();
                PostUpdatedEvent updatedEvent = new PostUpdatedEvent(postId, authorId, content, visibility, status, updatedAt);
                feedFanoutService.processPostUpdated(updatedEvent);
                return ;
            }
            Instant createdAt = root.has("createdAt") && !root.get("createdAt").isNull() ? Instant.parse(root.get("createdAt").asText()) : Instant.now();
            PostCreatedEvent createdEvent = new PostCreatedEvent(postId, authorId, content, visibility, status, createdAt);
            LOG.info("Processing PostCreatedEvent for postId: {} by authorId: {}", postId, authorId);
            feedFanoutService.processPostCreated(createdEvent);
        } catch (Exception e){
            LOG.error("Error parsing/processing post event payload '{}' : {}", payload, e.getMessage(), e);
        }
    }

}
