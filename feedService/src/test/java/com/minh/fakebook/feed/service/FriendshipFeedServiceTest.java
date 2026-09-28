package com.minh.fakebook.feed.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.minh.fakebook.feed.client.PostServiceClient;
import com.minh.fakebook.feed.client.dto.FeedPostReferenceDTO;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FriendshipFeedServiceTest {

    @Mock
    private PostServiceClient postServiceClient;

    @Mock
    private FriendshipFeedProjectionService projectionService;

    @Test
    void createdFriendshipBackfillsBothDirections() {
        UUID userId = UUID.randomUUID();
        UUID friendId = UUID.randomUUID();
        List<FeedPostReferenceDTO> userPosts = List.of(post(UUID.randomUUID(), userId));
        List<FeedPostReferenceDTO> friendPosts = List.of(post(UUID.randomUUID(), friendId));
        when(postServiceClient.getRecentFriendsPosts(userId, 500)).thenReturn(userPosts);
        when(postServiceClient.getRecentFriendsPosts(friendId, 500)).thenReturn(friendPosts);

        new FriendshipFeedService(postServiceClient, projectionService).processCreated(userId, friendId);

        verify(projectionService).backfill(userId, friendId, friendPosts);
        verify(projectionService).backfill(friendId, userId, userPosts);
    }

    @Test
    void deletedFriendshipRemovesBothDirections() {
        UUID userId = UUID.randomUUID();
        UUID friendId = UUID.randomUUID();

        new FriendshipFeedService(postServiceClient, projectionService).processDeleted(userId, friendId);

        verify(projectionService).removeFriendsPosts(userId, friendId);
        verify(projectionService).removeFriendsPosts(friendId, userId);
    }

    private FeedPostReferenceDTO post(UUID postId, UUID authorId) {
        return new FeedPostReferenceDTO(postId, authorId, "FRIENDS", Instant.parse("2026-09-28T00:00:00Z"));
    }
}
