package com.minh.fakebook.feed.service;

import com.minh.fakebook.feed.client.PostServiceClient;
import com.minh.fakebook.feed.client.dto.FeedPostReferenceDTO;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class FriendshipFeedService {

    private static final int BACKFILL_LIMIT = 500;

    private final PostServiceClient postServiceClient;
    private final FriendshipFeedProjectionService projectionService;

    public FriendshipFeedService(PostServiceClient postServiceClient, FriendshipFeedProjectionService projectionService) {
        this.postServiceClient = postServiceClient;
        this.projectionService = projectionService;
    }

    public void processCreated(UUID userId, UUID friendId) {
        List<FeedPostReferenceDTO> friendPosts = requireResponse(
            postServiceClient.getRecentFriendsPosts(friendId, BACKFILL_LIMIT)
        );
        List<FeedPostReferenceDTO> userPosts = requireResponse(
            postServiceClient.getRecentFriendsPosts(userId, BACKFILL_LIMIT)
        );

        projectionService.backfill(userId, friendId, friendPosts);
        projectionService.backfill(friendId, userId, userPosts);
    }

    public void processDeleted(UUID userId, UUID friendId) {
        projectionService.removeFriendsPosts(userId, friendId);
        projectionService.removeFriendsPosts(friendId, userId);
    }

    private List<FeedPostReferenceDTO> requireResponse(List<FeedPostReferenceDTO> posts) {
        if (posts == null) {
            throw new IllegalStateException("Post Service returned a null feed backfill response");
        }
        return posts;
    }
}
