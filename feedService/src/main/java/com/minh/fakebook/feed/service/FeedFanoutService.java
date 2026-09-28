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
        Set<UUID> targetUserIds = resolveTargetUserIds(event.authorId(), event.visibility());

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
        Set<UUID> recipientIds = new LinkedHashSet<>();
        items.forEach(item -> recipientIds.add(item.getUserId()));
        feedItemRepository.deleteByPostId(postId);
        removeFromRedisAfterCommit(recipientIds, postId);
        LOG.info("Successfully deleted DB and Redis feed items for post {}", postId);
    }

    /**
     * Processing post update events.
     */
    @Transactional
    public void processPostUpdated(PostUpdatedEvent event) {
        LOG.debug("Processing post update for postId: {}, visibility: {}", event.id(), event.visibility());

        if ("INACTIVE".equalsIgnoreCase(event.status())) {
            processPostDeleted(event.id());
            return;
        }

        Set<UUID> targetUserIds = resolveTargetUserIds(event.authorId(), event.visibility());
        List<FeedItem> existingItems = feedItemRepository.findByPostId(event.id());
        Set<UUID> previousRecipientIds = new LinkedHashSet<>();
        existingItems.forEach(item -> previousRecipientIds.add(item.getUserId()));

        feedItemRepository.deleteByPostId(event.id());
        Instant updatedAt = event.updatedAt() != null ? event.updatedAt() : Instant.now();
        for (UUID recipientId : targetUserIds) {
            feedItemRepository.insertIgnore(
                UUID.randomUUID().toString(),
                recipientId.toString(),
                event.id().toString(),
                updatedAt
            );
        }

        replaceRedisAfterCommit(previousRecipientIds, targetUserIds, event.id(), updatedAt);
    }

    private Set<UUID> resolveTargetUserIds(UUID authorId, String visibility) {
        Set<UUID> targetUserIds = new LinkedHashSet<>();
        targetUserIds.add(authorId);

        if (!"PRIVATE".equalsIgnoreCase(visibility)) {
            List<UUID> friendIds = userServiceClient.getUserFriendsList(authorId);
            if (friendIds != null) {
                targetUserIds.addAll(friendIds);
            }
        }
        if ("PUBLIC".equalsIgnoreCase(visibility)) {
            List<UUID> followerIds = userServiceClient.getUserFollowersList(authorId);
            if (followerIds != null) {
                targetUserIds.addAll(followerIds);
            }
        }

        return targetUserIds;
    }

    private void updateRedisAfterCommit(Set<UUID> recipientIds, UUID postId, Instant createdAt) {
        runAfterCommit(() -> updateRedis(recipientIds, postId, createdAt));
    }

    private void removeFromRedisAfterCommit(Set<UUID> recipientIds, UUID postId) {
        runAfterCommit(() -> removeFromRedis(recipientIds, postId));
    }

    private void replaceRedisAfterCommit(
        Set<UUID> previousRecipientIds,
        Set<UUID> targetRecipientIds,
        UUID postId,
        Instant updatedAt
    ) {
        runAfterCommit(() -> {
            removeFromRedis(previousRecipientIds, postId);
            updateRedis(targetRecipientIds, postId, updatedAt);
        });
    }

    private void runAfterCommit(Runnable update) {
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

    private void removeFromRedis(Set<UUID> recipientIds, UUID postId) {
        for (UUID recipientId : recipientIds) {
            redisTemplate.opsForZSet().remove(feedKey(recipientId), postId.toString());
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
