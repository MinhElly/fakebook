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

@IntegrationTest
class UserFeedDatabaseIT {

    @Autowired
    private UserFeedService userFeedService;

    @Autowired
    private FeedItemRepository feedItemRepository;


    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbc;

    @Test
    void existingTimelineIndexSupportsTheReadQuery() {
        testUserId = UUID.randomUUID();
        feedItemRepository.saveAndFlush(feedItem(testUserId, UUID.randomUUID(), Instant.now()));
        var indexes = jdbc.queryForList("SHOW INDEX FROM feed_items WHERE Key_name = 'idx_feed_items_user_created_post'");
        assertThat(indexes).extracting(row -> row.get("Column_name")).containsExactly("user_id", "created_at", "post_id");
        var plan = jdbc.queryForList(
            "EXPLAIN SELECT * FROM feed_items WHERE user_id = ? ORDER BY created_at DESC, post_id DESC LIMIT 20",
            testUserId.toString());
        org.slf4j.LoggerFactory.getLogger(getClass()).info("Feed timeline EXPLAIN: {}", plan);
        assertThat(plan).hasSize(1);
        assertThat(plan.getFirst().get("possible_keys").toString()).contains("idx_feed_items_user_created_post");
    }
    private UUID testUserId;

    @AfterEach
    void cleanUp() {
        feedItemRepository.deleteAll();
    }

    @Test
    void repeatedReadsPreserveDatabaseTimelineOrder() {
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

        var firstRead = userFeedService.getUserFeed(testUserId, PageRequest.of(0, 20));

        assertThat(firstRead.getContent()).extracting(FeedItemDTO::getPostId).containsExactly(higherPostId, lowerPostId);

        var secondRead = userFeedService.getUserFeed(testUserId, PageRequest.of(0, 20));

        assertThat(secondRead.getContent()).extracting(FeedItemDTO::getPostId).containsExactly(higherPostId, lowerPostId);
    }
    @Test
    void databaseReadReturnsAllProjectedItems() {
        testUserId = UUID.randomUUID();
        UUID olderPostId = UUID.randomUUID();
        UUID newerPostId = UUID.randomUUID();
        Instant olderTime = Instant.parse("2026-10-05T01:00:00Z");
        Instant newerTime = olderTime.plusSeconds(60);

        feedItemRepository.saveAllAndFlush(
            List.of(
                feedItem(testUserId, olderPostId, olderTime),
                feedItem(testUserId, newerPostId, newerTime)
            )
        );



        var result = userFeedService.getUserFeed(
            testUserId,
            PageRequest.of(0, 20)
        );

        assertThat(result.getContent())
            .extracting(FeedItemDTO::getPostId)
            .containsExactly(newerPostId, olderPostId);
    }

    private FeedItem feedItem(UUID userId, UUID postId, Instant createdAt) {
        return new FeedItem().userId(userId).postId(postId).createdAt(createdAt);
    }

}
