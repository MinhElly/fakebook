package com.minh.fakebook.feed.client;

import java.util.Collections;
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
        LOG.warn("Fallback triggered: userService is unavailable. Returning getUserFriendsList=null for userId: {}", userId);
        return Collections.emptyList();
    }
    
}
