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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for projecting post events into users' MariaDB news feeds.
 */
@Service
public class FeedFanoutService {

    private static final Logger LOG = LoggerFactory.getLogger(FeedFanoutService.class);

    private final UserServiceClient userServiceClient;
    private final FeedItemRepository feedItemRepository;

    public FeedFanoutService(
        UserServiceClient userServiceClient,
        FeedItemRepository feedItemRepository
    ) {
        this.userServiceClient = userServiceClient;
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
                event.authorId().toString(),
                event.visibility(),
                createdAt
            );
        }


        LOG.info("Fan-out for postId: {} completed. Processed {} recipients", event.id(), targetUserIds.size());
    }

    /**
     * Fan-out cleanup when a post is deleted.
     */
    @Transactional
    public void processPostDeleted(UUID postId) {
        LOG.debug("Removing feed items for deleted postId: {}", postId);
        feedItemRepository.deleteByPostId(postId);
        LOG.info("Successfully deleted database feed items for post {}", postId);
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

        feedItemRepository.deleteByPostId(event.id());
        Instant updatedAt = event.updatedAt() != null ? event.updatedAt() : Instant.now();
        for (UUID recipientId : targetUserIds) {
            feedItemRepository.insertIgnore(
                UUID.randomUUID().toString(),
                recipientId.toString(),
                event.id().toString(),
                event.authorId().toString(),
                event.visibility(),
                updatedAt
            );
        }

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

}
