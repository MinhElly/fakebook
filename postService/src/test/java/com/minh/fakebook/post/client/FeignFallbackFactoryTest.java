package com.minh.fakebook.post.client;

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
        assertThat(UserServiceClient.class.getAnnotation(FeignClient.class).fallbackFactory())
            .isEqualTo(UserServiceClientFallbackFactory.class);
        assertThat(MediaServiceClient.class.getAnnotation(FeignClient.class).fallbackFactory())
            .isEqualTo(MediaServiceClientFallbackFactory.class);
    }

    @Test
    void userServiceFallbackFailsClosedAndPreservesCause() {
        ConnectException cause = new ConnectException("connection refused");
        UserServiceClient client = new UserServiceClientFallbackFactory().create(cause);

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

        assertThatThrownBy(() -> new MediaServiceClientFallbackFactory().create(cause).getMedia(mediaId))
            .isInstanceOf(DownstreamServiceUnavailableException.class)
            .hasMessageContaining(mediaId.toString())
            .hasCause(cause);
    }

    @Test
    void fallbackPreservesFeignClientStatusInsteadOfReportingServiceUnavailable() {
        FeignException.NotFound cause = notFound("http://mediaservice/api/media/missing");

        Throwable thrown = catchThrowable(() ->
            new MediaServiceClientFallbackFactory().create(cause).getMedia(UUID.randomUUID())
        );

        assertThat(thrown).isInstanceOf(ResponseStatusException.class).hasCause(cause);
        assertThat(((ResponseStatusException) thrown).getStatusCode().value()).isEqualTo(404);
    }

    private FeignException.NotFound notFound(String url) {
        Request request = Request.create(Request.HttpMethod.GET, url, Map.of(), new byte[0], StandardCharsets.UTF_8);
        return new FeignException.NotFound("not found", request, new byte[0], Map.of());
    }
}
