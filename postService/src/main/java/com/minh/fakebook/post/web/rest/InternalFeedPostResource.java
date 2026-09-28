package com.minh.fakebook.post.web.rest;

import com.minh.fakebook.post.security.AuthoritiesConstants;
import com.minh.fakebook.post.service.FeedPostReferenceService;
import com.minh.fakebook.post.service.dto.FeedPostReferenceDTO;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/internal/feed-posts")
public class InternalFeedPostResource {

    private final FeedPostReferenceService feedPostReferenceService;

    public InternalFeedPostResource(FeedPostReferenceService feedPostReferenceService) {
        this.feedPostReferenceService = feedPostReferenceService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + AuthoritiesConstants.INTERNAL + "')")
    public List<FeedPostReferenceDTO> getRecentFriendsPosts(
        @RequestParam UUID authorId,
        @RequestParam(defaultValue = "500") @Min(1) @Max(500) int limit
    ) {
        return feedPostReferenceService.findRecentFriendsPosts(authorId, limit);
    }
}
