package com.minh.fakebook.feed.service;

import com.minh.fakebook.feed.client.UserServiceClient;
import com.minh.fakebook.feed.domain.FeedItem;
import com.minh.fakebook.feed.repository.FeedItemRepository;
import com.minh.fakebook.feed.service.event.PostCreatedEvent;
import com.minh.fakebook.feed.service.event.PostUpdatedEvent;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Service for fanning out post events to users' news feeds (Redis ZSet + MariaDB).
 */
@Service
public class FeedFanoutService {

    private static final Logger LOG = LoggerFactory.getLogger(FeedFanoutService.class);

    private final UserServiceClient userServiceClient;
    private final StringRedisTemplate redisTemplate;
    private final FeedItemRepository feedItemRepository;

    public FeedFanoutService(
        UserServiceClient userServiceClient,
        StringRedisTemplate redisTemplate,
        FeedItemRepository feedItemRepository
    ) {
        this.userServiceClient = userServiceClient;
        this.redisTemplate = redisTemplate;
        this.feedItemRepository = feedItemRepository;
    }

    @Transactional
    public void processPostCreated(PostCreatedEvent event) {
        LOG.debug("Processing fan-out for postId: {}, visibility: {}", event.id(), event.visibility());
        Set<UUID> targetUserIds = new LinkedHashSet<>();

        if ("PRIVATE".equalsIgnoreCase(event.visibility())) {
            targetUserIds.add(event.authorId());
        } else {
            List<UUID> friendIds = userServiceClient.getUserFriendsList(event.authorId());
            if (friendIds != null && !friendIds.isEmpty()) {
                targetUserIds.addAll(friendIds);
            }
        }
        if ("PUBLIC".equalsIgnoreCase(event.visibility())) {
            List<UUID> followerIds = userServiceClient.getUserFollowersList(event.authorId());
            if (followerIds != null && !followerIds.isEmpty()) {
                targetUserIds.addAll(followerIds);
            }
            targetUserIds.add(event.authorId());
        }

        if (targetUserIds.isEmpty()) {
            return;
        }

        Instant createdAt = event.createdAt() != null ? event.createdAt() : Instant.now();
        for (UUID recipientId : targetUserIds) {
            feedItemRepository.insertIgnore(
                UUID.randomUUID().toString(),
                recipientId.toString(),
                event.id().toString(),
                createdAt
            );
        }

        updateRedisAfterCommit(targetUserIds, event.id(), createdAt);

        LOG.info("Fan-out for postId: {} completed. Processed {} recipients", event.id(), targetUserIds.size());
    }

    /**
     * Fan-out cleanup when a post is deleted.
     */
    @Transactional
    public void processPostDeleted(UUID postId) {
        LOG.debug("Removing feed items for deleted postId: {}", postId);
        List<FeedItem> items = feedItemRepository.findByPostId(postId);
        for (FeedItem item : items) {
            redisTemplate.opsForZSet().remove(feedKey(item.getUserId()), postId.toString());
        }
        feedItemRepository.deleteByPostId(postId);
        LOG.info("Successfully deleted DB and Redis feed items for post {}", postId);
    }

        /**
     * Processing post update events.
     */
    @Transactional
    public void processPostUpdated(PostUpdatedEvent event) {
        LOG.debug("Processing post update for postId: {}, visibility: {}", event.id(), event.visibility());
        
        // Nếu bài viết đổi thành PRIVATE hoặc INACTIVE -> Xóa khỏi feed bạn bè
        if ("PRIVATE".equalsIgnoreCase(event.visibility()) || "INACTIVE".equalsIgnoreCase(event.status())) {
            processPostDeleted(event.id());
        }
    }

    private void updateRedisAfterCommit(Set<UUID> recipientIds, UUID postId, Instant createdAt) {
        Runnable update = () -> updateRedis(recipientIds, postId, createdAt);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        update.run();
                    }
                }
            );
        } else {
            update.run();
        }
    }

    private void updateRedis(Set<UUID> recipientIds, UUID postId, Instant createdAt) {
        double score = createdAt.toEpochMilli();
        for (UUID recipientId : recipientIds) {
            String redisKey = feedKey(recipientId);
            redisTemplate.opsForZSet().add(redisKey, postId.toString(), score);
            redisTemplate.opsForZSet().removeRange(redisKey, 0, -501);
        }
    }

    private String feedKey(UUID userId) {
        return "feed:user:" + userId;
    }

}
