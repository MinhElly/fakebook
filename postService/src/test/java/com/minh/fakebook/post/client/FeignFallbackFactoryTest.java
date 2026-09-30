package com.minh.fakebook.post.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.ConnectException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.openfeign.FeignClient;

class FeignFallbackFactoryTest {

    @Test
    void clientsUseCauseAwareFallbackFactories() {
        assertThat(UserServiceClient.class.getAnnotation(FeignClient.class).fallbackFactory())
            .isEqualTo(UserServiceClientFallback.class);
        assertThat(MediaServiceClient.class.getAnnotation(FeignClient.class).fallbackFactory())
            .isEqualTo(MediaServiceClientFallback.class);
    }

    @Test
    void userServiceFallbackFailsClosedAndPreservesCause() {
        ConnectException cause = new ConnectException("connection refused");
        UserServiceClient client = new UserServiceClientFallback().create(cause);

        assertThatThrownBy(() -> client.areFriends(UUID.randomUUID(), UUID.randomUUID()))
            .isInstanceOf(DownstreamServiceUnavailableException.class)
            .hasMessageContaining("checking friendship")
            .hasCause(cause);

        assertThatThrownBy(() -> client.getFriendIdsByUserId(UUID.randomUUID()))
            .isInstanceOf(DownstreamServiceUnavailableException.class)
            .hasMessageContaining("loading friend IDs")
            .hasCause(cause);
    }

    @Test
    void mediaServiceFallbackFailsClosedAndPreservesCause() {
        ConnectException cause = new ConnectException("connection refused");
        UUID mediaId = UUID.randomUUID();

        assertThatThrownBy(() -> new MediaServiceClientFallback().create(cause).getMedia(mediaId))
            .isInstanceOf(DownstreamServiceUnavailableException.class)
            .hasMessageContaining(mediaId.toString())
            .hasCause(cause);
    }
}
