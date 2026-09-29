package com.minh.fakebook.user.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.ConnectException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.openfeign.FeignClient;

class MediaServiceFallbackFactoryTest {

    @Test
    void clientUsesCauseAwareFallbackFactory() {
        assertThat(MediaServiceClient.class.getAnnotation(FeignClient.class).fallbackFactory())
            .isEqualTo(MediaServiceClientFallback.class);
    }

    @Test
    void fallbackFailsClosedAndPreservesCause() {
        ConnectException cause = new ConnectException("connection refused");
        UUID mediaId = UUID.randomUUID();

        assertThatThrownBy(() -> new MediaServiceClientFallback().create(cause).getMediaById(mediaId))
            .isInstanceOf(DownstreamServiceUnavailableException.class)
            .hasMessageContaining(mediaId.toString())
            .hasCause(cause);
    }
}
