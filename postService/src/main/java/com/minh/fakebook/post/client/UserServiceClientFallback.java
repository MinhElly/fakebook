package com.minh.fakebook.post.client;

import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class UserServiceClientFallback implements FallbackFactory<UserServiceClient> {

    private static final Logger LOG = LoggerFactory.getLogger(UserServiceClientFallback.class);

    @Override
    public UserServiceClient create(Throwable cause) {
        return new UserServiceClient() {
            @Override
            public boolean areFriends(UUID userId1, UUID userId2) {
                throw unavailable("checking friendship", cause);
            }

            @Override
            public List<UUID> getFriendIdsByUserId(UUID userId) {
                throw unavailable("loading friend IDs", cause);
            }
        };
    }

    private DownstreamServiceUnavailableException unavailable(String operation, Throwable cause) {
        LOG.error("User Service unavailable during {}", operation, cause);
        return new DownstreamServiceUnavailableException("User Service", operation, cause);
    }
}

