package com.minh.fakebook.feed.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.minh.fakebook.feed.IntegrationTest;
import com.minh.fakebook.feed.client.UserServiceClient;
import com.minh.fakebook.feed.client.UserServiceUnavailableException;
import com.minh.fakebook.feed.client.dto.FeedPostReferenceDTO;
import com.minh.fakebook.feed.domain.FeedItem;
import com.minh.fakebook.feed.repository.FeedItemRepository;
import com.minh.fakebook.feed.service.event.PostCreatedEvent;
import com.minh.fakebook.feed.service.event.PostUpdatedEvent;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@IntegrationTest
class FeedPersistenceIT {

    private static final Instant CREATED_AT = Instant.parse("2026-09-22T10:00:00Z");

    @Autowired
    private FeedFanoutService feedFanoutService;

    @Autowired
    private FriendshipFeedProjectionService friendshipFeedProjectionService;

    @Autowired
    private FeedItemRepository feedItemRepository;

    @MockitoBean
    private UserServiceClient userServiceClient;

    @AfterEach
    void cleanUp() {
        feedItemRepository.deleteAll();
        reset(userServiceClient);
    }

    @Test
    void createEventPersistsExactlyOneRowPerDistinctRecipientAndDuplicateDeliveryIsSafe() {
        UUID authorId = UUID.randomUUID();
        UUID friendId = UUID.randomUUID();
        UUID followerId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        PostCreatedEvent event = postCreatedEvent(postId, authorId, "PUBLIC");
        when(userServiceClient.getUserFriendsList(authorId)).thenReturn(List.of(friendId));
        when(userServiceClient.getUserFollowersList(authorId)).thenReturn(List.of(friendId, followerId));

        feedFanoutService.processPostCreated(event);
        feedFanoutService.processPostCreated(event);

        List<FeedItem> rows = feedItemRepository.findByPostId(postId);
        assertThat(rows).extracting(FeedItem::getUserId).containsExactlyInAnyOrder(authorId, friendId, followerId);
        assertThat(rows).hasSize(3);
        assertThat(rows)
            .allSatisfy(item -> {
                assertThat(item.getAuthorId()).isEqualTo(authorId);
                assertThat(item.getVisibility()).isEqualTo("PUBLIC");
            });
    }

    @Test
    void friendsEventPersistsForAuthorAndFriendsButNotFollowers() {
        UUID authorId = UUID.randomUUID();
        UUID friendId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        when(userServiceClient.getUserFriendsList(authorId)).thenReturn(List.of(friendId));

        feedFanoutService.processPostCreated(postCreatedEvent(postId, authorId, "FRIENDS"));

        assertThat(feedItemRepository.findByPostId(postId))
            .extracting(FeedItem::getUserId)
            .containsExactlyInAnyOrder(authorId, friendId);
        assertThat(feedItemRepository.findByPostId(postId))
            .allSatisfy(item -> {
                assertThat(item.getAuthorId()).isEqualTo(authorId);
                assertThat(item.getVisibility()).isEqualTo("FRIENDS");
            });
        verify(userServiceClient, never()).getUserFollowersList(authorId);
    }

    @Test
    void audienceLookupFailurePropagatesWithoutWritingFeedRows() {
        UUID authorId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        RuntimeException rootCause = new RuntimeException("connection refused");
        when(userServiceClient.getUserFriendsList(authorId))
            .thenThrow(new UserServiceUnavailableException("userService unavailable", rootCause));

        assertThatThrownBy(() -> feedFanoutService.processPostCreated(postCreatedEvent(postId, authorId, "FRIENDS")))
            .isInstanceOf(UserServiceUnavailableException.class)
            .hasCause(rootCause);

        assertThat(feedItemRepository.findByPostId(postId)).isEmpty();
    }

    @Test
    void deleteRemovesMariaDbAndRedisAndCanBeRepeated() {
        UUID userId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        feedItemRepository.saveAndFlush(new FeedItem().userId(userId).postId(postId).createdAt(CREATED_AT));

        feedFanoutService.processPostDeleted(postId);
        assertThatCode(() -> feedFanoutService.processPostDeleted(postId)).doesNotThrowAnyException();

        assertThat(feedItemRepository.findByPostId(postId)).isEmpty();
    }

    @Test
    void updateToPrivateKeepsOnlyTheAuthorInTheFeed() {
        UUID authorId = UUID.randomUUID();
        UUID friendId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        feedItemRepository.saveAllAndFlush(
            List.of(
                new FeedItem().userId(authorId).postId(postId).createdAt(CREATED_AT),
                new FeedItem().userId(friendId).postId(postId).createdAt(CREATED_AT)
            )
        );

        feedFanoutService.processPostUpdated(
            new PostUpdatedEvent(postId, authorId, "content", "PRIVATE", "ACTIVE", CREATED_AT.plusSeconds(60))
        );

        assertThat(feedItemRepository.findByPostId(postId))
            .extracting(FeedItem::getUserId)
            .containsExactly(authorId);
        assertThat(feedItemRepository.findByPostId(postId))
            .allSatisfy(item -> {
                assertThat(item.getAuthorId()).isEqualTo(authorId);
                assertThat(item.getVisibility()).isEqualTo("PRIVATE");
            });
        verify(userServiceClient, never()).getUserFriendsList(authorId);
        verify(userServiceClient, never()).getUserFollowersList(authorId);
    }

    @Test
    void mariaDbPaginationUsesPostIdAsTieBreaker() {
        UUID userId = UUID.randomUUID();
        UUID lowerPostId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID higherPostId = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");
        feedItemRepository.saveAllAndFlush(
            List.of(
                new FeedItem().userId(userId).postId(lowerPostId).createdAt(CREATED_AT),
                new FeedItem().userId(userId).postId(higherPostId).createdAt(CREATED_AT)
            )
        );

        var page = feedItemRepository.findByUserIdOrderByCreatedAtDescPostIdDesc(userId, PageRequest.of(0, 20));

        assertThat(page.getContent()).extracting(FeedItem::getPostId).containsExactly(higherPostId, lowerPostId);
    }

    @Test
    void findsAndDeletesOnlyFriendsItemsByRecipientAndAuthor() {
        UUID recipientId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID friendsPostId = UUID.randomUUID();
        UUID publicPostId = UUID.randomUUID();
        feedItemRepository.saveAllAndFlush(
            List.of(
                new FeedItem()
                    .userId(recipientId)
                    .postId(friendsPostId)
                    .authorId(authorId)
                    .visibility("FRIENDS")
                    .createdAt(CREATED_AT),
                new FeedItem()
                    .userId(recipientId)
                    .postId(publicPostId)
                    .authorId(authorId)
                    .visibility("PUBLIC")
                    .createdAt(CREATED_AT.plusSeconds(1))
            )
        );

        assertThat(feedItemRepository.findByUserIdAndAuthorIdAndVisibility(recipientId, authorId, "FRIENDS"))
            .extracting(FeedItem::getPostId)
            .containsExactly(friendsPostId);

        feedItemRepository.deleteByUserIdAndAuthorIdAndVisibility(recipientId, authorId, "FRIENDS");
        feedItemRepository.flush();

        assertThat(feedItemRepository.findByPostId(friendsPostId)).isEmpty();
        assertThat(feedItemRepository.findByPostId(publicPostId)).hasSize(1);
    }

    @Test
    void friendshipBackfillPersistsMetadataAndIsIdempotent() {
        UUID recipientId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        var post = new FeedPostReferenceDTO(postId, authorId, "FRIENDS", CREATED_AT);

        friendshipFeedProjectionService.backfill(recipientId, authorId, List.of(post));
        friendshipFeedProjectionService.backfill(recipientId, authorId, List.of(post));

        assertThat(feedItemRepository.findByUserIdAndAuthorIdAndVisibility(recipientId, authorId, "FRIENDS"))
            .singleElement()
            .satisfies(item -> {
                assertThat(item.getPostId()).isEqualTo(postId);
                assertThat(item.getCreatedAt()).isEqualTo(CREATED_AT);
            });
    }

    @Test
    void unfriendingRemovesOnlyFriendsProjectionFromMariaDb() {
        UUID recipientId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID friendsPostId = UUID.randomUUID();
        UUID publicPostId = UUID.randomUUID();
        feedItemRepository.saveAllAndFlush(
            List.of(
                new FeedItem()
                    .userId(recipientId)
                    .postId(friendsPostId)
                    .authorId(authorId)
                    .visibility("FRIENDS")
                    .createdAt(CREATED_AT),
                new FeedItem()
                    .userId(recipientId)
                    .postId(publicPostId)
                    .authorId(authorId)
                    .visibility("PUBLIC")
                    .createdAt(CREATED_AT.plusSeconds(1))
            )
        );

        friendshipFeedProjectionService.removeFriendsPosts(recipientId, authorId);

        assertThat(feedItemRepository.findByPostId(friendsPostId)).isEmpty();
        assertThat(feedItemRepository.findByPostId(publicPostId)).hasSize(1);
    }

    private PostCreatedEvent postCreatedEvent(UUID postId, UUID authorId, String visibility) {
        return new PostCreatedEvent(postId, authorId, "content", visibility, "ACTIVE", CREATED_AT);
    }
}
