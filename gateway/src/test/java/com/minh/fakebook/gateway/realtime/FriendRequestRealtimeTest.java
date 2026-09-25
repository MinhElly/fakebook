package com.minh.fakebook.gateway.realtime;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import com.minh.fakebook.gateway.web.rest.RealtimeResource;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.oauth2.jwt.Jwt;

class FriendRequestRealtimeTest {

    @Test
    void shouldConvertFriendRequestEventAndDeliverItOnlyToItsRecipient() throws Exception {
        UUID recipientId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        RealtimeEventHub eventHub = new RealtimeEventHub();
        RealtimeResource resource = new RealtimeResource(eventHub);
        FriendRequestEventConsumerConfiguration configuration = new FriendRequestEventConsumerConfiguration();

        CompletableFuture<ServerSentEvent<RealtimeEvent>> recipientEvent = resource
            .events(jwtFor(recipientId))
            .filter(event -> event.data() != null)
            .next()
            .toFuture();
        CompletableFuture<ServerSentEvent<RealtimeEvent>> otherUserEvent = resource
            .events(jwtFor(otherUserId))
            .filter(event -> event.data() != null)
            .next()
            .toFuture();

        configuration
            .friendRequestEventConsumer(eventHub)
            .accept(
                new FriendRequestGatewayEvent(
                    UUID.randomUUID(),
                    "FRIEND_REQUEST_CREATED",
                    1,
                    Instant.now(),
                    new FriendRequestGatewayEvent.Data(requestId, senderId, recipientId)
                )
            );

        ServerSentEvent<RealtimeEvent> delivered = recipientEvent.get(1, SECONDS);
        assertThat(delivered.event()).isEqualTo("friend-request-created");
        assertThat(delivered.data()).isNotNull();
        assertThat(delivered.data().requestId()).isEqualTo(requestId);
        assertThat(delivered.data().actorUserId()).isEqualTo(senderId);
        assertThat(delivered.data().recipientId()).isEqualTo(recipientId);
        assertThat(otherUserEvent).isNotDone();

        otherUserEvent.cancel(true);
    }

    private Jwt jwtFor(UUID userId) {
        return new Jwt(
            "token",
            Instant.now(),
            Instant.now().plusSeconds(60),
            Map.of("alg", "none"),
            Map.of("sub", userId.toString())
        );
    }
}
