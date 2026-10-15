package com.minh.fakebook.user.web.rest;
import com.minh.fakebook.user.repository.*;
import java.util.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/internal/social-context")
@PreAuthorize("hasAuthority('ROLE_INTERNAL')")
public class SocialContextResource {
    private final FriendshipRepository friends;
    private final FollowRepository follows;
    public SocialContextResource(FriendshipRepository friends, FollowRepository follows) { this.friends = friends; this.follows = follows; }
    public record SocialContext(Set<UUID> friends, Set<UUID> following) {}
    @GetMapping("/{userId}")
    public SocialContext read(@PathVariable UUID userId) {
        return new SocialContext(new HashSet<>(friends.findFriendIdsByUserId(userId)), new HashSet<>(follows.findFollowingIdsByUserId(userId)));
    }
    @GetMapping("/{userId}/friend-ids")
    public List<UUID> friendIds(@PathVariable UUID userId) {
        return friends.findFriendIdsByUserId(userId);
    }

    @GetMapping("/{userId}/follower-ids")
    public List<UUID> followerIds(@PathVariable UUID userId) {
        return follows.findFollowerIdsByUserId(userId);
    }
}
