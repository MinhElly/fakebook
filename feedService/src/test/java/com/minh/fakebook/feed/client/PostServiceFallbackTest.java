package com.minh.fakebook.feed.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.openfeign.FeignClient;

class PostServiceFallbackTest {

    private final PostServiceFallback fallbackFactory = new PostServiceFallback();

    @Test
    void shouldConfigureFallbackFactory() {
        FeignClient annotation = PostServiceClient.class.getAnnotation(FeignClient.class);

        assertThat(annotation.fallbackFactory()).isEqualTo(PostServiceFallback.class);
    }

    @Test
    void shouldFailBackfillWhenPostServiceIsUnavailable() {
        RuntimeException cause = new RuntimeException("connection refused");
        UUID authorId = UUID.randomUUID();

        assertThatThrownBy(() -> fallbackFactory.create(cause).getRecentFriendsPosts(authorId, 500))
            .isInstanceOf(PostServiceUnavailableException.class)
            .hasMessageContaining("friendship feed backfill")
            .hasMessageContaining(authorId.toString())
            .hasCause(cause);
    }
}
