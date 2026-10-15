package com.minh.fakebook.feed.client;
import java.time.Instant;
import java.util.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "postservice", contextId = "feedReadClient", configuration = TokenRelayRequestInterceptor.class)
public interface FeedReadClient {
    record Candidate(UUID postId, UUID authorId, String visibility, Instant createdAt) {}
    record CandidateRequest(UUID viewerId, Set<UUID> friends, Set<UUID> following, String source, Instant asOf, String cursor, int limit) {}
    record CandidatePage(List<Candidate> items, String nextCursor, boolean hasMore) {}
    record ReadRequest(List<UUID> ids, UUID viewerId, Set<UUID> friends) {}
    @PostMapping("/api/internal/feed-posts/candidates") CandidatePage candidates(@RequestBody CandidateRequest request);
    @PostMapping("/api/internal/feed-posts/readable") List<Candidate> readable(@RequestBody ReadRequest request);
}
