package com.minh.fakebook.feed.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import feign.FeignException;
import feign.Request;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.openfeign.FeignClient;

class PostServiceFallbackFactoryTest {

    private final PostServiceFallbackFactory fallbackFactory = new PostServiceFallbackFactory();

    @Test
    void shouldConfigureFallbackFactory() {
        FeignClient annotation = PostServiceClient.class.getAnnotation(FeignClient.class);

        assertThat(annotation.fallbackFactory()).isEqualTo(PostServiceFallbackFactory.class);
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

    @Test
    void shouldPreserveFeignClientExceptionForKafkaRetryAndDlt() {
        FeignException.NotFound cause = notFound("http://postservice/api/internal/feed-posts");

        assertThatThrownBy(() -> fallbackFactory.create(cause).getRecentFriendsPosts(UUID.randomUUID(), 500)).isSameAs(cause);
    }

    private FeignException.NotFound notFound(String url) {
        Request request = Request.create(Request.HttpMethod.GET, url, Map.of(), new byte[0], StandardCharsets.UTF_8);
        return new FeignException.NotFound("not found", request, new byte[0], Map.of());
    }
}
