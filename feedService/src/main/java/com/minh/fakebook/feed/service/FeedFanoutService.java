package com.minh.fakebook.feed.service;

import com.minh.fakebook.feed.repository.FeedItemRepository;
import com.minh.fakebook.feed.service.client.UserServiceClient;
import com.minh.fakebook.feed.service.dto.FeedItemDTO;
import com.minh.fakebook.feed.service.dto.event.PostCreatedEvent;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for fanning out post events to users' news feeds (Redis ZSet + MariaDB).
 */
@Service
public class FeedFanoutService {

    private static final Logger LOG = LoggerFactory.getLogger(FeedFanoutService.class);

    private final UserServiceClient userServiceClient;
    private final StringRedisTemplate redisTemplate;
    private final FeedItemService feedItemService;
    private final FeedItemRepository feedItemRepository;

    public FeedFanoutService(
        UserServiceClient userServiceClient,
        StringRedisTemplate redisTemplate,
        FeedItemService feedItemService,
        FeedItemRepository feedItemRepository
    ) {
        this.userServiceClient = userServiceClient;
        this.redisTemplate = redisTemplate;
        this.feedItemService = feedItemService;
        this.feedItemRepository = feedItemRepository;
    }

    /**
     * Async & Transactional Fan-out processing on new post creation.
     */
    @Async
    @Transactional
    public void processPostCreated(PostCreatedEvent event) {
        LOG.debug("Processing fan-out for postId: {}, visibility: {}", event.getPostId(), event.getVisibility());
        List<UUID> targetUserIds = new ArrayList<>();

        if ("PRIVATE".equalsIgnoreCase(event.getVisibility())) {
            targetUserIds.add(event.getAuthorId());
        } else {
            try {
                List<UUID> friendIds = userServiceClient.getUserFriendsList(event.getAuthorId());
                if (friendIds != null && !friendIds.isEmpty()) {
                    targetUserIds.addAll(friendIds);
                }
            } catch (Exception e) {
                LOG.error("Failed to fetch friends list for user {}", event.getAuthorId(), e);
            }
            if (!targetUserIds.contains(event.getAuthorId())) {
                targetUserIds.add(event.getAuthorId());
            }
        }

        if (targetUserIds.isEmpty()) {
            return;
        }

        Instant createdAt = event.getCreatedAt() != null ? event.getCreatedAt() : Instant.now();
        double score = createdAt.toEpochMilli();
        String postIdStr = event.getPostId().toString();
        int maxFeedSize = 500;

        // 1. Redis ZSet Fan-out for O(1) Feed Reading
        for (UUID recipientId : targetUserIds) {
            String redisKey = "feed:user:" + recipientId.toString();
            try {
                redisTemplate.opsForZSet().add(redisKey, postIdStr, score);
                redisTemplate.opsForZSet().removeRange(redisKey, 0, -(maxFeedSize + 1));
            } catch (Exception e) {
                LOG.warn("Failed to update Redis ZSet feed for user {}: {}", recipientId, e.getMessage());
            }
        }

        // 2. MariaDB Persistence
        for (UUID recipientId : targetUserIds) {
            FeedItemDTO dto = new FeedItemDTO();
            dto.setUserId(recipientId);
            dto.setPostId(event.getPostId());
            dto.setCreatedAt(createdAt);
            feedItemService.save(dto);
        }

        LOG.info("Fan-out for postId: {} completed. Processed {} recipients", event.getPostId(), targetUserIds.size());
    }

    /**
     * Fan-out cleanup when a post is deleted.
     */
    @Async
    @Transactional
    public void processPostDeleted(UUID postId) {
        LOG.debug("Removing feed items for deleted postId: {}", postId);
        feedItemRepository.deleteByPostId(postId);
        LOG.info("Successfully deleted DB feed items for post {}", postId);
    }
}
