package com.minh.fakebook.feed.client;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component 
public class UserServiceFallback implements UserServiceClient{

    private static final Logger LOG = LoggerFactory.getLogger(UserServiceFallback.class);

    @Override
    public List<UUID> getUserFriendsList(UUID userId) {
        LOG.error("Fallback triggered while loading friends for userId: {}", userId);
        throw new IllegalStateException("userService is unavailable while loading friends for " + userId);
    }

    @Override
    public List<UUID> getUserFollowersList(UUID userId) {
        LOG.error("Fallback triggered while loading followers for userId: {}", userId);
        throw new IllegalStateException("userService is unavailable while loading followers for " + userId);
    }
    
}
