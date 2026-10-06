package com.minh.fakebook.post.client;

import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class UserServiceClientFallbackFactory implements FallbackFactory<UserServiceClient> {

    private static final Logger LOG = LoggerFactory.getLogger(UserServiceClientFallbackFactory.class);

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

    private RuntimeException unavailable(String operation, Throwable cause) {
        RuntimeException failure = DownstreamFailureMapper.map("User Service", operation, cause);
        if (failure instanceof org.springframework.web.server.ResponseStatusException) {
            LOG.debug("User Service rejected request during {}", operation);
        } else if (DownstreamFailureMapper.isCircuitOpen(cause)) {
            LOG.warn("User Service Circuit Breaker is OPEN during {}", operation);
        } else {
            LOG.error("User Service unavailable during {}", operation, cause);
        }
        return failure;
    }
}

