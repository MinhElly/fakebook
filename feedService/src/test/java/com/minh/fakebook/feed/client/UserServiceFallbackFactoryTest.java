package com.minh.fakebook.feed.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import feign.FeignException;
import feign.Request;
import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.openfeign.FeignClient;

class UserServiceFallbackFactoryTest {

    private final UserServiceFallbackFactory fallbackFactory = new UserServiceFallbackFactory();

    @Test
    void feignClientUsesCauseAwareFallbackFactory() {
        FeignClient annotation = UserServiceClient.class.getAnnotation(FeignClient.class);

        assertThat(annotation.fallbackFactory()).isEqualTo(UserServiceFallbackFactory.class);
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

    @Test
    void fallbackPreservesFeignClientExceptionForKafkaRetryAndDlt() {
        FeignException.NotFound cause = notFound("http://userservice/api/friendships/missing");

        assertThatThrownBy(() -> fallbackFactory.create(cause).getUserFriendsList(UUID.randomUUID())).isSameAs(cause);
    }

    private FeignException.NotFound notFound(String url) {
        Request request = Request.create(Request.HttpMethod.GET, url, Map.of(), new byte[0], StandardCharsets.UTF_8);
        return new FeignException.NotFound("not found", request, new byte[0], Map.of());
    }
}
