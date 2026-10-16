package com.minh.fakebook.gateway.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Signal;
import tools.jackson.databind.json.JsonMapper;

class UserPresenceClientTest {

    private static final Instant LAST_SEEN =
        Instant.parse("2026-10-16T03:05:00Z");

    private final AtomicInteger responseStatus =
        new AtomicInteger(204);

    private final AtomicReference<CapturedRequest> captured =
        new AtomicReference<>();

    private HttpServer server;
    private InternalServiceTokenProvider tokenProvider;
    private UserPresenceClient client;

    private record CapturedRequest(
        String method,
        String path,
        String authorization,
        String contentType,
        String body
    ) {}

    @BeforeEach
    void setUp() throws Exception {
        tokenProvider = mock(InternalServiceTokenProvider.class);

        when(tokenProvider.getAccessToken())
            .thenReturn(Mono.just("test-service-token"));

        server = HttpServer.create(
            new InetSocketAddress("127.0.0.1", 0),
            0
        );

        server.createContext("/", exchange -> {
            try {
                String body = new String(
                    exchange.getRequestBody().readAllBytes(),
                    StandardCharsets.UTF_8
                );

                captured.set(new CapturedRequest(
                    exchange.getRequestMethod(),
                    exchange.getRequestURI().getPath(),
                    exchange.getRequestHeaders()
                        .getFirst("Authorization"),
                    exchange.getRequestHeaders()
                        .getFirst("Content-Type"),
                    body
                ));

                exchange.sendResponseHeaders(
                    responseStatus.get(),
                    -1
                );
            } finally {
                exchange.close();
            }
        });

        server.start();

        String baseUrl = "http://127.0.0.1:"
            + server.getAddress().getPort();

        client = new UserPresenceClient(
            WebClient.builder(),
            tokenProvider,
            baseUrl
        );
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void shouldSendExpectedRequestAndAccept204() {
        UUID userId = UUID.randomUUID();

        Signal<Void> result = execute(userId);

        assertThat(result.isOnComplete()).isTrue();

        CapturedRequest request = captured.get();

        assertThat(request).isNotNull();
        assertThat(request.method()).isEqualTo("PUT");
        assertThat(request.path()).isEqualTo(
            "/api/internal/presence/" + userId + "/last-seen"
        );
        assertThat(request.authorization())
            .isEqualTo("Bearer test-service-token");
        assertThat(request.contentType())
            .startsWith("application/json");

        var json = JsonMapper.builder()
            .build()
            .readTree(request.body());

        assertThat(json.size()).isEqualTo(1);
        assertThat(json.get("lastSeenAt").asText())
            .isEqualTo(LAST_SEEN.toString());
    }

    @Test
    void shouldPropagateHttpErrors() {
        for (int status : List.of(400, 401, 403, 404, 500)) {
            responseStatus.set(status);

            Signal<Void> result = execute(UUID.randomUUID());

            assertThat(result.getThrowable())
                .isInstanceOf(WebClientResponseException.class);

            var error =
                (WebClientResponseException) result.getThrowable();

            assertThat(error.getStatusCode().value())
                .isEqualTo(status);
        }
    }

    @Test
    void shouldRejectUnexpectedSuccessStatus() {
        responseStatus.set(200);

        Signal<Void> result = execute(UUID.randomUUID());

        assertThat(result.getThrowable())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("200");
    }

    @Test
    void shouldStopBeforeHttpWhenTokenAcquisitionFails() {
        var tokenError =
            new IllegalStateException("Token acquisition failed");

        when(tokenProvider.getAccessToken())
            .thenReturn(Mono.error(tokenError));

        Signal<Void> result = execute(UUID.randomUUID());

        assertThat(result.getThrowable()).isSameAs(tokenError);
        assertThat(captured.get()).isNull();
    }

    @Test
    void shouldTimeoutWhenTokenAcquisitionNeverCompletes() {
        when(tokenProvider.getAccessToken())
            .thenReturn(Mono.never());

        Signal<Void> result = execute(UUID.randomUUID());

        assertThat(result.getThrowable())
            .isInstanceOf(TimeoutException.class);
        assertThat(captured.get()).isNull();
    }

    private Signal<Void> execute(UUID userId) {
        Signal<Void> result = client
            .recordLastSeen(userId, LAST_SEEN)
            .materialize()
            .block(Duration.ofSeconds(10));

        assertThat(result).isNotNull();

        return result;
    }
}
