package com.minh.fakebook.feed.web.rest;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.minh.fakebook.feed.service.UserFeedService;
import com.minh.fakebook.feed.service.dto.FeedItemDTO;

@RestController 
@RequestMapping("/api/feed")
public class UserFeedResource {
    private static final Logger LOG = LoggerFactory.getLogger(UserFeedResource.class);

    private final UserFeedService userFeedService;

    public UserFeedResource(UserFeedService userFeedService) {
        this.userFeedService = userFeedService;
    }

    @GetMapping ("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<FeedItemDTO>> getCurrentUserFeed(Pageable pageable){
        LOG.debug("REST request to get current user feed");
        UUID currentUserId = null;
        if(SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken jwtAuth){
            String sub = jwtAuth.getToken().getSubject();
            currentUserId = UUID.fromString(sub);
        }
        if(currentUserId == null){
            return ResponseEntity.badRequest().build(); 
        }
        Page<FeedItemDTO> page = userFeedService.getUserFeed(currentUserId, pageable);
        return ResponseEntity.ok().body(page.getContent());
    }
}
