package com.minh.fakebook.comment.client;

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
        return (userId1, userId2) -> {
            RuntimeException failure = DownstreamFailureMapper.map("User Service", "checking friendship", cause);
            if (failure instanceof org.springframework.web.server.ResponseStatusException) {
                LOG.debug("User Service rejected friendship check between {} and {}", userId1, userId2);
            } else if (DownstreamFailureMapper.isCircuitOpen(cause)) {
                LOG.warn("User Service Circuit Breaker is OPEN while checking friendship between {} and {}", userId1, userId2);
            } else {
                LOG.error("User Service unavailable while checking friendship between {} and {}", userId1, userId2, cause);
            }
            throw failure;
        };
    }
}
