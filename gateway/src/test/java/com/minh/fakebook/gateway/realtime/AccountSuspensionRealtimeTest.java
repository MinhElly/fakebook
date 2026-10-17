package com.minh.fakebook.gateway.realtime;

import static org.mockito.Mockito.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import com.minh.fakebook.gateway.administration.AccountDirectory;
import com.minh.fakebook.gateway.web.rest.RealtimeResource;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class AccountSuspensionRealtimeTest {
    @Test void suspensionStopsNextSseFrameEvenWithExistingConnection() {
        UUID viewer=UUID.randomUUID();var hub=new RealtimeEventHub();var directory=mock(AccountDirectory.class);
        when(directory.community(anyString(),any())).thenReturn(Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN,"ACCOUNT_SUSPENDED")));
        var jwt=Jwt.withTokenValue("verified").header("alg","RS256").subject(viewer.toString()).issuedAt(Instant.now()).build();
        // Heartbeats are checked too; a quiet connection cannot retain an unchecked authenticated stream.
        StepVerifier.withVirtualTime(()->new RealtimeResource(hub,directory).events(jwt))
            .thenAwait(java.time.Duration.ofSeconds(21)).expectErrorMatches(error -> error instanceof ResponseStatusException status && status.getStatusCode().value()==403)
            .verify(java.time.Duration.ofSeconds(5));
    }
}
