package com.minh.fakebook.gateway.client;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class UserPresenceClient {

    private static final Duration REQUEST_TIMEOUT =
        Duration.ofSeconds(5);

    private final WebClient webClient;
    private final InternalServiceTokenProvider tokenProvider;

    public UserPresenceClient(
        WebClient.Builder builder,
        InternalServiceTokenProvider tokenProvider,
        @Value("${USER_SERVICE_URL:http://localhost:8082}")
        String userServiceUrl
    ) {
        this.webClient = builder.clone()
            .baseUrl(userServiceUrl)
            .build();

        this.tokenProvider = tokenProvider;
    }

    public record LastSeenRequest(Instant lastSeenAt) {}

    public record ReadRequest(UUID viewerId, java.util.Set<UUID> userIds) {}
    public record ReadablePresence(UUID userId, Instant lastSeenAt) {}

    /** Always checks the authoritative friendship table; never cache authorization. */
    public Mono<java.util.List<ReadablePresence>> readable(UUID viewerId, java.util.Set<UUID> userIds) {
        return Mono.defer(() -> tokenProvider.getAccessToken().flatMap(token -> webClient.post()
            .uri("/api/internal/presence/readable")
            .headers(headers -> headers.setBearerAuth(token))
            .bodyValue(new ReadRequest(viewerId, userIds))
            .retrieve().bodyToFlux(ReadablePresence.class).collectList()))
            .timeout(REQUEST_TIMEOUT);
    }

    public Mono<Void> recordLastSeen(
        UUID userId,
        Instant lastSeenAt
    ) {
        return Mono.defer(() -> {
            Objects.requireNonNull(
                userId,
                "userId must not be null"
            );
            Objects.requireNonNull(
                lastSeenAt,
                "lastSeenAt must not be null"
            );

            return tokenProvider.getAccessToken()
                .flatMap(token ->
                    webClient.put()
                        .uri(
                            "/api/internal/presence/{userId}/last-seen",
                            userId
                        )
                        .headers(headers ->
                            headers.setBearerAuth(token)
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(
                            new LastSeenRequest(lastSeenAt)
                        )
                        .retrieve()
                        .toBodilessEntity()
                )
                .flatMap(response -> {
                    if (response.getStatusCode().value() != 204) {
                        return Mono.<Void>error(
                            new IllegalStateException(
                                "Unexpected presence response status: "
                                    + response.getStatusCode().value()
                            )
                        );
                    }

                    return Mono.<Void>empty();
                });
        }).timeout(REQUEST_TIMEOUT);
    }
}
