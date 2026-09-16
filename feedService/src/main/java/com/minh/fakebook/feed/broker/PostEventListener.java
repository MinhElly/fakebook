package com.minh.fakebook.feed.broker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;

import com.minh.fakebook.feed.service.FeedFanoutService;
import com.minh.fakebook.feed.service.dto.event.PostCreatedEvent;

public class PostEventListener {
    private static final Logger LOG = LoggerFactory.getLogger(PostEventListener.class);
    private final FeedFanoutService feedFanoutService;
    public PostEventListener(FeedFanoutService feedFanoutService){
        this.feedFanoutService = feedFanoutService;
    }
    @KafkaListener(topics="post-events", groupId = "feed-service-group")
    public void handlePostCreatedEvent(PostCreatedEvent event){
        LOG.debug("Received PostCreatedEvent for postId: {}", event.getPostId());
        feedFanoutService.processPostCreated(event);
    }
}
