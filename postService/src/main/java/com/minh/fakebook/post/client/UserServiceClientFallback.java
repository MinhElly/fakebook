package com.minh.fakebook.post.client;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component 
public class UserServiceClientFallback implements UserServiceClient {

    private static final Logger LOG = LoggerFactory.getLogger(UserServiceClientFallback.class);

    @Override
    public boolean areFriends(UUID userId1, UUID userId2) {
        LOG.warn("Fallback triggered: userService is unavailable. Returning areFriends=false for userId1: {}, userId2: {}", userId1,userId2);
        return false;
    }
    
}
