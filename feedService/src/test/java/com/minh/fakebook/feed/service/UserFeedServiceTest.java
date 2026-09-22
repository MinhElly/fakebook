package com.minh.fakebook.feed.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.minh.fakebook.feed.domain.FeedItem;
import com.minh.fakebook.feed.repository.FeedItemRepository;
import com.minh.fakebook.feed.service.dto.FeedItemDTO;
import com.minh.fakebook.feed.service.mapper.FeedItemMapper;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

@ExtendWith(MockitoExtension.class)
class UserFeedServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

    @Mock
    private FeedItemRepository feedItemRepository;

    @Mock
    private FeedItemMapper feedItemMapper;

    private UserFeedService userFeedService;

    @BeforeEach
    void setUp() {
        userFeedService = new UserFeedService(redisTemplate, feedItemRepository, feedItemMapper);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(feedItemMapper.toDto(any(FeedItem.class))).thenAnswer(invocation -> toDto(invocation.getArgument(0)));
    }

    @Test
    void preservesRedisOrderWhenMariaDbReturnsItemsInAnotherOrder() {
        UUID userId = UUID.randomUUID();
        UUID firstPostId = UUID.randomUUID();
        UUID secondPostId = UUID.randomUUID();
        var pageable = PageRequest.of(0, 20);
        String redisKey = "feed:user:" + userId;
        Set<String> cachedIds = new LinkedHashSet<>(List.of(firstPostId.toString(), secondPostId.toString()));
        FeedItem first = feedItem(userId, firstPostId, Instant.parse("2026-09-22T10:00:00Z"));
        FeedItem second = feedItem(userId, secondPostId, Instant.parse("2026-09-22T09:00:00Z"));

        when(zSetOperations.reverseRange(redisKey, 0, 19)).thenReturn(cachedIds);
        when(zSetOperations.zCard(redisKey)).thenReturn(2L);
        when(feedItemRepository.findByUserIdAndPostIdIn(userId, List.of(firstPostId, secondPostId)))
            .thenReturn(List.of(second, first));

        var result = userFeedService.getUserFeed(userId, pageable);

        assertThat(result.getContent()).extracting(FeedItemDTO::getPostId).containsExactly(firstPostId, secondPostId);
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    void fallsBackToMariaDbOnRedisMiss() {
        UUID userId = UUID.randomUUID();
        var pageable = PageRequest.of(1, 10);
        FeedItem item = feedItem(userId, UUID.randomUUID(), Instant.parse("2026-09-22T10:00:00Z"));

        when(zSetOperations.reverseRange("feed:user:" + userId, 10, 19)).thenReturn(Set.of());
        when(feedItemRepository.findByUserIdOrderByCreatedAtDescPostIdDesc(userId, pageable))
            .thenReturn(new PageImpl<>(List.of(item), pageable, 11));

        var result = userFeedService.getUserFeed(userId, pageable);

        assertThat(result.getContent()).extracting(FeedItemDTO::getPostId).containsExactly(item.getPostId());
        assertThat(result.getTotalElements()).isEqualTo(11);
    }

    @Test
    void fallsBackToMariaDbWhenRedisIsUnavailable() {
        UUID userId = UUID.randomUUID();
        var pageable = PageRequest.of(0, 20);
        FeedItem item = feedItem(userId, UUID.randomUUID(), Instant.parse("2026-09-22T10:00:00Z"));

        when(zSetOperations.reverseRange("feed:user:" + userId, 0, 19)).thenThrow(new IllegalStateException("Redis unavailable"));
        when(feedItemRepository.findByUserIdOrderByCreatedAtDescPostIdDesc(userId, pageable))
            .thenReturn(new PageImpl<>(List.of(item), pageable, 1));

        var result = userFeedService.getUserFeed(userId, pageable);

        assertThat(result.getContent()).extracting(FeedItemDTO::getPostId).containsExactly(item.getPostId());
    }

    @Test
    void removesRedisMembersThatNoLongerExistInMariaDb() {
        UUID userId = UUID.randomUUID();
        UUID existingPostId = UUID.randomUUID();
        UUID stalePostId = UUID.randomUUID();
        var pageable = PageRequest.of(0, 20);
        String redisKey = "feed:user:" + userId;
        Set<String> cachedIds = new LinkedHashSet<>(List.of(existingPostId.toString(), stalePostId.toString()));
        FeedItem existing = feedItem(userId, existingPostId, Instant.parse("2026-09-22T10:00:00Z"));

        when(zSetOperations.reverseRange(redisKey, 0, 19)).thenReturn(cachedIds);
        when(feedItemRepository.findByUserIdAndPostIdIn(userId, List.of(existingPostId, stalePostId))).thenReturn(List.of(existing));
        when(zSetOperations.zCard(redisKey)).thenReturn(1L);

        var result = userFeedService.getUserFeed(userId, pageable);

        assertThat(result.getContent()).extracting(FeedItemDTO::getPostId).containsExactly(existingPostId);
        verify(zSetOperations).remove(redisKey, stalePostId.toString());
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    private FeedItem feedItem(UUID userId, UUID postId, Instant createdAt) {
        return new FeedItem().id(UUID.randomUUID()).userId(userId).postId(postId).createdAt(createdAt);
    }

    private FeedItemDTO toDto(FeedItem item) {
        FeedItemDTO dto = new FeedItemDTO();
        dto.setId(item.getId());
        dto.setUserId(item.getUserId());
        dto.setPostId(item.getPostId());
        dto.setCreatedAt(item.getCreatedAt());
        return dto;
    }
}
