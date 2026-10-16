package com.minh.fakebook.gateway.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;
import com.minh.fakebook.gateway.IntegrationTest;
import com.minh.fakebook.gateway.presence.PresenceTickets;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.web.reactive.server.WebTestClient;

@IntegrationTest
@AutoConfigureWebTestClient(timeout = IntegrationTest.DEFAULT_TIMEOUT)
class PresenceResourceIT {
    @Autowired WebTestClient http;
    @Autowired PresenceTickets tickets;

    @Test
    void ticketRequiresAuthentication() {
        http.post().uri("/api/presence/tickets").exchange().expectStatus().isUnauthorized();
    }

    @Test
    void ticketUsesJwtSubjectAndNeverAllowsCaching() {
        UUID subject = UUID.randomUUID();
        var response = http.mutateWith(mockJwt().jwt(jwt -> jwt.subject(subject.toString()).expiresAt(Instant.now().plusSeconds(300))))
            .post().uri("/api/presence/tickets").exchange().expectStatus().isOk()
            .expectHeader().valueMatches("Cache-Control", ".*no-store.*")
            .expectBody(PresenceTickets.Ticket.class).returnResult().getResponseBody();
        assertThat(response).isNotNull();
        assertThat(response.expiresAt()).isBeforeOrEqualTo(Instant.now().plusSeconds(30));
        assertThat(tickets.consume(response.ticket()).block().userId()).isEqualTo(subject);
    }

    @Test
    void expiredOrNonUuidIdentityCannotObtainTicket() {
        http.mutateWith(mockJwt().jwt(jwt -> jwt.subject(UUID.randomUUID().toString()).expiresAt(Instant.now().minusSeconds(1))))
            .post().uri("/api/presence/tickets").exchange().expectStatus().isUnauthorized();
        http.mutateWith(mockJwt().jwt(jwt -> jwt.subject("not-an-account-id").expiresAt(Instant.now().plusSeconds(300))))
            .post().uri("/api/presence/tickets").exchange().expectStatus().isUnauthorized();
    }

    @Test
    void websocketRouteRequiresUpgradeAndDoesNotRedirectToLogin() {
        http.get().uri("/api/presence/ws").exchange().expectStatus().isBadRequest();
    }
}
