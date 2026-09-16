package com.minh.fakebook.feed.web.rest;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.minh.fakebook.feed.security.SecurityUtils;
import com.minh.fakebook.feed.service.dto.FeedItemDTO;

@RestController 
@RequestMapping("/api/feed")
public class UserFeedResource {
    private static final Logger LOG = LoggerFactory.getLogger(UserFeedResource.class);

    private final UserFeedService userFeedService;

    public UserFeedResource(UserFeedService userFeedService) {
        this.userFeedService = userFeedService;
    }

    public ResponseEntity<List<FeedItemDTO>> getCurrentUserFeed(Pageable pageable){
        LOG.debug("REST request to get current user feed");
        UUID currentUserId = SecurityUtils.getCurrentUserLogin();
    }
}
