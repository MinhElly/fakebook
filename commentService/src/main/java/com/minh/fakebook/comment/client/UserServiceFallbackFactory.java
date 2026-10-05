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
            LOG.error("User Service unavailable while checking friendship between {} and {}", userId1, userId2, cause);
            throw new DownstreamServiceUnavailableException("User Service", "checking friendship", cause);
        };
    }
}
