package com.minh.fakebook.comment.client;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component 
public class UserServiceFallback implements UserServiceClient{

    private static final Logger LOG = LoggerFactory.getLogger(UserServiceFallback.class);
    @Override
    public boolean checkFriendship(UUID userId1, UUID userId2) {
        LOG.warn("Fallback triggered: userService is unavailable. Returning checkFriendship=false for userId1: {}, userId2: {}", userId1,userId2);
        return false;
    }
}
