package com.minh.fakebook.feed.client;

import com.minh.fakebook.feed.client.dto.FeedPostReferenceDTO;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class PostServiceFallbackFactory implements FallbackFactory<PostServiceClient> {

    private static final Logger LOG = LoggerFactory.getLogger(PostServiceFallbackFactory.class);

    @Override
    public PostServiceClient create(Throwable cause) {
        return new PostServiceClient() {
            @Override
            public List<FeedPostReferenceDTO> getRecentFriendsPosts(UUID authorId, int limit) {
                LOG.error("Post Service unavailable during friendship feed backfill for author {}", authorId, cause);
                throw new PostServiceUnavailableException(
                    "Post Service unavailable during friendship feed backfill for author " + authorId,
                    cause
                );
            }
        };
    }
}
