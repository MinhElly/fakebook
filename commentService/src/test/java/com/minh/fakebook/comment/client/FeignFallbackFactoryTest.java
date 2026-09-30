package com.minh.fakebook.comment.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.ConnectException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.openfeign.FeignClient;

class FeignFallbackFactoryTest {

    @Test
    void clientsUseCauseAwareFallbackFactories() {
        assertThat(UserServiceClient.class.getAnnotation(FeignClient.class).fallbackFactory()).isEqualTo(UserServiceFallback.class);
        assertThat(PostFeignClient.class.getAnnotation(FeignClient.class).fallbackFactory())
            .isEqualTo(PostFeignClientFallback.class);
    }

    @Test
    void userServiceFallbackFailsClosedAndPreservesCause() {
        ConnectException cause = new ConnectException("connection refused");

        assertThatThrownBy(() ->
            new UserServiceFallback().create(cause).checkFriendship(UUID.randomUUID(), UUID.randomUUID())
        )
            .isInstanceOf(DownstreamServiceUnavailableException.class)
            .hasMessageContaining("checking friendship")
            .hasCause(cause);
    }

    @Test
    void postServiceFallbackFailsClosedAndPreservesCause() {
        ConnectException cause = new ConnectException("connection refused");
        UUID postId = UUID.randomUUID();

        assertThatThrownBy(() -> new PostFeignClientFallback().create(cause).getPostById(postId))
            .isInstanceOf(DownstreamServiceUnavailableException.class)
            .hasMessageContaining(postId.toString())
            .hasCause(cause);
    }
}
