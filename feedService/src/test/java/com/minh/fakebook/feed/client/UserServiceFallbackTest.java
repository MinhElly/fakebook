package com.minh.fakebook.feed.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.ConnectException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.openfeign.FeignClient;

class UserServiceFallbackTest {

    private final UserServiceFallback fallbackFactory = new UserServiceFallback();

    @Test
    void feignClientUsesCauseAwareFallbackFactory() {
        FeignClient annotation = UserServiceClient.class.getAnnotation(FeignClient.class);

        assertThat(annotation.fallbackFactory()).isEqualTo(UserServiceFallback.class);
    }

    @Test
    void friendsFallbackFailsClosedAndPreservesCause() {
        ConnectException cause = new ConnectException("connection refused");
        UUID userId = UUID.randomUUID();

        assertThatThrownBy(() -> fallbackFactory.create(cause).getUserFriendsList(userId))
            .isInstanceOf(UserServiceUnavailableException.class)
            .hasMessageContaining("friends lookup")
            .hasMessageContaining(userId.toString())
            .hasCause(cause);
    }

    @Test
    void followersFallbackFailsClosedAndPreservesCause() {
        IllegalStateException cause = new IllegalStateException("circuit is open");
        UUID userId = UUID.randomUUID();

        assertThatThrownBy(() -> fallbackFactory.create(cause).getUserFollowersList(userId))
            .isInstanceOf(UserServiceUnavailableException.class)
            .hasMessageContaining("followers lookup")
            .hasMessageContaining(userId.toString())
            .hasCause(cause);
    }
}
