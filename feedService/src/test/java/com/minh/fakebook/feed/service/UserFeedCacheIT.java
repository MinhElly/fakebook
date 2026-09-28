package com.minh.fakebook.feed.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.minh.fakebook.feed.IntegrationTest;
import com.minh.fakebook.feed.domain.FeedItem;
import com.minh.fakebook.feed.repository.FeedItemRepository;
import com.minh.fakebook.feed.service.dto.FeedItemDTO;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;

@IntegrationTest
class UserFeedCacheIT {

    @Autowired
    private UserFeedService userFeedService;

    @Autowired
    private FeedItemRepository feedItemRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private UUID testUserId;

    @AfterEach
    void cleanUp() {
        feedItemRepository.deleteAll();
        if (testUserId != null) {
            redisTemplate.delete(feedKey(testUserId));
        }
    }

    @Test
    void cacheMissWarmsRedisAndTheNextReadPreservesTimelineOrder() {
        testUserId = UUID.randomUUID();
        UUID lowerPostId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID higherPostId = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");
        Instant createdAt = Instant.parse("2026-09-28T01:00:00Z");
        feedItemRepository.saveAllAndFlush(
            List.of(
                feedItem(testUserId, lowerPostId, createdAt),
                feedItem(testUserId, higherPostId, createdAt)
            )
        );
        redisTemplate.delete(feedKey(testUserId));

        var firstRead = userFeedService.getUserFeed(testUserId, PageRequest.of(0, 20));

        assertThat(firstRead.getContent()).extracting(FeedItemDTO::getPostId).containsExactly(higherPostId, lowerPostId);
        assertThat(redisTemplate.opsForZSet().reverseRange(feedKey(testUserId), 0, -1))
            .containsExactly(higherPostId.toString(), lowerPostId.toString());

        var secondRead = userFeedService.getUserFeed(testUserId, PageRequest.of(0, 20));

        assertThat(secondRead.getContent()).extracting(FeedItemDTO::getPostId).containsExactly(higherPostId, lowerPostId);
    }

    private FeedItem feedItem(UUID userId, UUID postId, Instant createdAt) {
        return new FeedItem().userId(userId).postId(postId).createdAt(createdAt);
    }

    private String feedKey(UUID userId) {
        return "feed:user:" + userId;
    }
}
