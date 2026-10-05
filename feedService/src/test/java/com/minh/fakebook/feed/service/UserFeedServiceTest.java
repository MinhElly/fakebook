package com.minh.fakebook.feed.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.minh.fakebook.feed.domain.FeedItem;
import com.minh.fakebook.feed.repository.FeedItemRepository;
import com.minh.fakebook.feed.service.dto.FeedItemDTO;
import com.minh.fakebook.feed.service.mapper.FeedItemMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class UserFeedServiceTest {

    @Mock
    private FeedItemRepository feedItemRepository;

    @Mock
    private FeedItemMapper feedItemMapper;

    private UserFeedService userFeedService;

    @BeforeEach
    void setUp() {
        userFeedService = new UserFeedService(feedItemRepository, feedItemMapper);
    }

    @Test
    void preservesMariaDbPageOrderAndTotal() {
        when(feedItemMapper.toDto(any(FeedItem.class))).thenAnswer(invocation -> toDto(invocation.getArgument(0)));
        UUID userId = UUID.randomUUID();
        var pageable = PageRequest.of(0, 20);
        FeedItem first = feedItem(userId, UUID.randomUUID(), Instant.parse("2026-09-22T10:00:00Z"));
        FeedItem second = feedItem(userId, UUID.randomUUID(), Instant.parse("2026-09-22T09:00:00Z"));

        when(feedItemRepository.findByUserIdOrderByCreatedAtDescPostIdDesc(userId, pageable))
            .thenReturn(new PageImpl<>(List.of(first, second), pageable, 600));

        var result = userFeedService.getUserFeed(userId, pageable);

        assertThat(result.getContent()).extracting(FeedItemDTO::getPostId).containsExactly(first.getPostId(), second.getPostId());
        assertThat(result.getTotalElements()).isEqualTo(600);
        verify(feedItemRepository).findByUserIdOrderByCreatedAtDescPostIdDesc(userId, pageable);
        verifyNoMoreInteractions(feedItemRepository);
    }

    @Test
    void returnsRequestedPageWithoutAnAdditionalWarmQuery() {
        when(feedItemMapper.toDto(any(FeedItem.class))).thenAnswer(invocation -> toDto(invocation.getArgument(0)));
        UUID userId = UUID.randomUUID();
        var pageable = PageRequest.of(1, 10);
        FeedItem item = feedItem(userId, UUID.randomUUID(), Instant.parse("2026-09-22T10:00:00Z"));

        when(feedItemRepository.findByUserIdOrderByCreatedAtDescPostIdDesc(userId, pageable))
            .thenReturn(new PageImpl<>(List.of(item), pageable, 11));

        var result = userFeedService.getUserFeed(userId, pageable);

        assertThat(result.getContent()).extracting(FeedItemDTO::getPostId).containsExactly(item.getPostId());
        assertThat(result.getTotalElements()).isEqualTo(11);
        verify(feedItemRepository).findByUserIdOrderByCreatedAtDescPostIdDesc(userId, pageable);
        verifyNoMoreInteractions(feedItemRepository);

    }

    @Test
    void returnsPageBeyondFiveHundredItems() {
        when(feedItemMapper.toDto(any(FeedItem.class))).thenAnswer(invocation -> toDto(invocation.getArgument(0)));
        UUID userId = UUID.randomUUID();
        var pageable = PageRequest.of(25, 20);
        FeedItem item = feedItem(userId, UUID.randomUUID(), Instant.parse("2026-09-22T10:00:00Z"));

        when(feedItemRepository.findByUserIdOrderByCreatedAtDescPostIdDesc(userId, pageable))
            .thenReturn(new PageImpl<>(List.of(item), pageable, 501));

        var result = userFeedService.getUserFeed(userId, pageable);

        assertThat(result.getContent()).extracting(FeedItemDTO::getPostId).containsExactly(item.getPostId());
        assertThat(result.getTotalElements()).isEqualTo(501);
        verify(feedItemRepository).findByUserIdOrderByCreatedAtDescPostIdDesc(userId, pageable);
        verifyNoMoreInteractions(feedItemRepository);
    }

    @Test
    void emptyFeedReturnsEmptyPage() {
        UUID userId = UUID.randomUUID();
        var pageable = PageRequest.of(0, 20);
        when(feedItemRepository.findByUserIdOrderByCreatedAtDescPostIdDesc(userId, pageable))
            .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        var result = userFeedService.getUserFeed(userId, pageable);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
        verifyNoInteractions(feedItemMapper);
        verify(feedItemRepository).findByUserIdOrderByCreatedAtDescPostIdDesc(userId, pageable);
        verifyNoMoreInteractions(feedItemRepository);
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
