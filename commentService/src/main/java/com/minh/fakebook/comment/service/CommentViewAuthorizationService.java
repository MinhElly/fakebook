package com.minh.fakebook.comment.service;

import com.minh.fakebook.comment.client.UserServiceClient;
import com.minh.fakebook.comment.domain.PostCache;
import com.minh.fakebook.comment.security.AuthoritiesConstants;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
public class CommentViewAuthorizationService {

    private final UserServiceClient userServiceClient;

    private final PostCacheResolver postCacheResolver;

    public CommentViewAuthorizationService(UserServiceClient userServiceClient, PostCacheResolver postCacheResolver) {
        this.userServiceClient = userServiceClient;
        this.postCacheResolver = postCacheResolver;
    }

    public void verifyCanView(Collection<UUID> postIds) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = authentication
            .getAuthorities()
            .stream()
            .anyMatch(authority -> authority.getAuthority().equals(AuthoritiesConstants.ADMIN));

        if (isAdmin) {
            return;
        }

        UUID currentUserId = getCurrentUserId(authentication);
        Set<UUID> checkedFriendAuthors = new HashSet<>();

        for (UUID postId : postIds) {
            PostCache post = postCacheResolver.resolve(postId);
            if (currentUserId.equals(post.getAuthorId())) {
                continue;
            }
            if ("PRIVATE".equalsIgnoreCase(post.getVisibility())) {
                throw new AccessDeniedException("You cannot view comments of this private post");
            }
            if (
                "FRIENDS".equalsIgnoreCase(post.getVisibility()) &&
                checkedFriendAuthors.add(post.getAuthorId()) &&
                !userServiceClient.checkFriendship(currentUserId, post.getAuthorId())
            ) {
                throw new AccessDeniedException("You must be friends to view comments of this post");
            }
        }
    }

    private UUID getCurrentUserId(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
            return UUID.fromString(jwtAuthentication.getToken().getSubject());
        }
        throw new AccessDeniedException("User not authenticated properly");
    }
}
