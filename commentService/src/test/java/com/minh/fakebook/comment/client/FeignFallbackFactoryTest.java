package com.minh.fakebook.comment.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

import feign.FeignException;
import feign.Request;
import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.server.ResponseStatusException;

class FeignFallbackFactoryTest {

    @Test
    void clientsUseCauseAwareFallbackFactories() {
        assertThat(UserServiceClient.class.getAnnotation(FeignClient.class).fallbackFactory()).isEqualTo(UserServiceFallbackFactory.class);
        assertThat(PostFeignClient.class.getAnnotation(FeignClient.class).fallbackFactory())
            .isEqualTo(PostFeignClientFallbackFactory.class);
    }

    @Test
    void userServiceFallbackFailsClosedAndPreservesCause() {
        ConnectException cause = new ConnectException("connection refused");

        assertThatThrownBy(() ->
            new UserServiceFallbackFactory().create(cause).checkFriendship(UUID.randomUUID(), UUID.randomUUID())
        )
            .isInstanceOf(DownstreamServiceUnavailableException.class)
            .hasMessageContaining("checking friendship")
            .hasCause(cause);
    }

    @Test
    void postServiceFallbackFailsClosedAndPreservesCause() {
        ConnectException cause = new ConnectException("connection refused");
        UUID postId = UUID.randomUUID();

        assertThatThrownBy(() -> new PostFeignClientFallbackFactory().create(cause).getPostById(postId))
            .isInstanceOf(DownstreamServiceUnavailableException.class)
            .hasMessageContaining(postId.toString())
            .hasCause(cause);
    }

    @Test
    void fallbackPreservesFeignClientStatusInsteadOfReportingServiceUnavailable() {
        FeignException.NotFound cause = notFound("http://postservice/api/posts/missing");

        Throwable thrown = catchThrowable(() ->
            new PostFeignClientFallbackFactory().create(cause).getPostById(UUID.randomUUID())
        );

        assertThat(thrown).isInstanceOf(ResponseStatusException.class).hasCause(cause);
        assertThat(((ResponseStatusException) thrown).getStatusCode().value()).isEqualTo(404);
    }

    private FeignException.NotFound notFound(String url) {
        Request request = Request.create(Request.HttpMethod.GET, url, Map.of(), new byte[0], StandardCharsets.UTF_8);
        return new FeignException.NotFound("not found", request, new byte[0], Map.of());
    }
}
