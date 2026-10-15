package com.minh.fakebook.post.web.rest;
import com.minh.fakebook.post.service.PostBrowseService;
import java.util.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/internal/feed-posts")
@PreAuthorize("hasAuthority('ROLE_INTERNAL')")
public class FeedCandidatesResource {
    private final PostBrowseService browse;
    public FeedCandidatesResource(PostBrowseService browse) { this.browse = browse; }
    public record ReadRequest(List<UUID> ids, UUID viewerId, Set<UUID> friends) {}
    @PostMapping("/candidates")
    public PostBrowseService.CandidatePage candidates(@RequestBody PostBrowseService.CandidateRequest request) { return browse.candidates(request); }
    @PostMapping("/readable")
    public List<PostBrowseService.Candidate> readable(@RequestBody ReadRequest request) {
        if (request.ids() == null || request.ids().size() > 100) throw new IllegalArgumentException("At most 100 post IDs");
        return browse.readable(request.ids(), request.viewerId(), request.friends());
    }
}
