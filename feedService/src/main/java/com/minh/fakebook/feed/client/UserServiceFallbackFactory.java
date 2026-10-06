package com.minh.fakebook.feed.client;

import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class UserServiceFallbackFactory implements FallbackFactory<UserServiceClient> {

    private static final Logger LOG = LoggerFactory.getLogger(UserServiceFallbackFactory.class);

    @Override
    public UserServiceClient create(Throwable cause) {
        return new UserServiceClient() {
            @Override
            public List<UUID> getUserFriendsList(UUID userId) {
                throw unavailable("friends", userId, cause);
            }

            @Override
            public List<UUID> getUserFollowersList(UUID userId) {
                throw unavailable("followers", userId, cause);
            }
        };
    }

    private UserServiceUnavailableException unavailable(String operation, UUID userId, Throwable cause) {
        FeignFailureHandler.rethrowClientError(cause);
        if (FeignFailureHandler.isCircuitOpen(cause)) {
            LOG.warn("User Service Circuit Breaker is OPEN during {} lookup for user {}", operation, userId);
        } else {
            LOG.error("User Service unavailable during {} lookup for user {}", operation, userId, cause);
        }
        return new UserServiceUnavailableException(
            "User Service unavailable during " + operation + " lookup for user " + userId,
            cause
        );
    }
}
