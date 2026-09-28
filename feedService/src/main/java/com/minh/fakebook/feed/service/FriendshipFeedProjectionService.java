package com.minh.fakebook.feed.service;

import com.minh.fakebook.feed.domain.FeedItem;
import com.minh.fakebook.feed.repository.FeedItemRepository;
import com.minh.fakebook.feed.client.dto.FeedPostReferenceDTO;
import java.util.List;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class FriendshipFeedProjectionService {

    private static final String FRIENDS_VISIBILITY = "FRIENDS";

    private final FeedItemRepository feedItemRepository;
    private final StringRedisTemplate redisTemplate;

    public FriendshipFeedProjectionService(FeedItemRepository feedItemRepository, StringRedisTemplate redisTemplate) {
        this.feedItemRepository = feedItemRepository;
        this.redisTemplate = redisTemplate;
    }

    @Transactional
    public void backfill(UUID recipientId, UUID expectedAuthorId, List<FeedPostReferenceDTO> posts) {
        for (FeedPostReferenceDTO post : posts) {
            validatePost(post, expectedAuthorId);
            feedItemRepository.insertIgnore(
                UUID.randomUUID().toString(),
                recipientId.toString(),
                post.postId().toString(),
                post.authorId().toString(),
                post.visibility(),
                post.createdAt()
            );
        }

        runAfterCommit(() -> {
            for (FeedPostReferenceDTO post : posts) {
                redisTemplate
                    .opsForZSet()
                    .add(feedKey(recipientId), post.postId().toString(), post.createdAt().toEpochMilli());
            }
            redisTemplate.opsForZSet().removeRange(feedKey(recipientId), 0, -501);
        });
    }

    @Transactional
    public void removeFriendsPosts(UUID recipientId, UUID authorId) {
        List<FeedItem> items = feedItemRepository.findByUserIdAndAuthorIdAndVisibility(
            recipientId,
            authorId,
            FRIENDS_VISIBILITY
        );
        feedItemRepository.deleteByUserIdAndAuthorIdAndVisibility(recipientId, authorId, FRIENDS_VISIBILITY);

        runAfterCommit(() -> {
            for (FeedItem item : items) {
                redisTemplate.opsForZSet().remove(feedKey(recipientId), item.getPostId().toString());
            }
        });
    }

    private void validatePost(FeedPostReferenceDTO post, UUID expectedAuthorId) {
        if (
            post == null ||
            post.postId() == null ||
            post.authorId() == null ||
            post.createdAt() == null ||
            !expectedAuthorId.equals(post.authorId()) ||
            !FRIENDS_VISIBILITY.equalsIgnoreCase(post.visibility())
        ) {
            throw new IllegalArgumentException("Post Service returned invalid FRIENDS feed metadata");
        }
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

    private String feedKey(UUID userId) {
        return "feed:user:" + userId;
    }
}
