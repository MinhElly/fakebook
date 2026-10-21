package com.minh.fakebook.media.security;

import static org.assertj.core.api.Assertions.*;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class PlaybackTicketsTest {
    private JwtAuthenticationToken auth(String subject, String token) {
        return new JwtAuthenticationToken(Jwt.withTokenValue(token).header("alg","RS256").subject(subject)
            .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(120)).build());
    }
    @Test void scopedLeaseRenewsWithoutExposingBearerAndCannotBeStolenByAnotherOwner() {
        var service = new PlaybackTickets(); var id = UUID.randomUUID();
        String ticket = service.issue(id, auth("one", "secret-token"), null);
        assertThat(ticket).doesNotContain("secret-token");
        assertThat(service.bearer(id, ticket)).isEqualTo("secret-token");
        assertThat(service.bearer(UUID.randomUUID(), ticket)).isNull();
        assertThat(service.issue(id, auth("one", "refreshed-token"), ticket)).isEqualTo(ticket);
        assertThat(service.bearer(id, ticket)).isEqualTo("refreshed-token");
        assertThat(service.issue(id, auth("two", "other-token"), ticket)).isNotEqualTo(ticket);
    }
}
