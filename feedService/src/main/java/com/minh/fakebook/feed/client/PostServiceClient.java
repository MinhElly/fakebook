package com.minh.fakebook.feed.client;

import com.minh.fakebook.feed.client.dto.FeedPostReferenceDTO;
import java.util.List;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
    name = "postservice",
    configuration = TokenRelayRequestInterceptor.class,
    fallbackFactory = PostServiceFallbackFactory.class
)
public interface PostServiceClient {
    @GetMapping("/api/internal/feed-posts")
    List<FeedPostReferenceDTO> getRecentFriendsPosts(
        @RequestParam("authorId") UUID authorId,
        @RequestParam("limit") int limit
    );
}
