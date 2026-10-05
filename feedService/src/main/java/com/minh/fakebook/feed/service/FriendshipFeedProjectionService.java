package com.minh.fakebook.feed.service;

import com.minh.fakebook.feed.domain.FeedItem;
import com.minh.fakebook.feed.repository.FeedItemRepository;
import com.minh.fakebook.feed.client.dto.FeedPostReferenceDTO;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FriendshipFeedProjectionService {

    private static final String FRIENDS_VISIBILITY = "FRIENDS";

    private final FeedItemRepository feedItemRepository;

    public FriendshipFeedProjectionService(FeedItemRepository feedItemRepository) {
        this.feedItemRepository = feedItemRepository;
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

    }

    @Transactional
    public void removeFriendsPosts(UUID recipientId, UUID authorId) {
        feedItemRepository.deleteByUserIdAndAuthorIdAndVisibility(recipientId, authorId, FRIENDS_VISIBILITY);

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

}
