package com.minh.fakebook.feed.client;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import feign.RequestTemplate;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.*;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class TokenRelayRequestInterceptorTest {
    final OAuth2AuthorizedClientManager manager = mock(OAuth2AuthorizedClientManager.class);
    final TokenRelayRequestInterceptor interceptor = new TokenRelayRequestInterceptor(manager);

    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @Test void internalCallNeverRelaysEndUserJwt() {
        authenticate();
        var registration = ClientRegistration.withRegistrationId("internal").clientId("feed")
            .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS).tokenUri("http://issuer/token").build();
        var access = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER,"service-token",Instant.now(),Instant.now().plusSeconds(60));
        when(manager.authorize(any())).thenReturn(new OAuth2AuthorizedClient(registration,"feed-service",access));
        var request = new RequestTemplate().uri("/api/internal/feed-posts/candidates");
        interceptor.apply(request);
        assertThat(request.headers().get("Authorization")).containsExactly("Bearer service-token");
    }

    @Test void businessCallRelaysJwtAndMissingServiceTokenFailsClosed() {
        authenticate();
        var business = new RequestTemplate().uri("/api/posts/public");
        interceptor.apply(business);
        assertThat(business.headers().get("Authorization")).containsExactly("Bearer user-token");
        verifyNoInteractions(manager);
        assertThatThrownBy(() -> interceptor.apply(new RequestTemplate().uri("/api/internal/feed-posts/candidates")))
            .isInstanceOf(IllegalStateException.class);
    }

    private void authenticate() {
        var jwt = Jwt.withTokenValue("user-token").header("alg","none").subject(UUID.randomUUID().toString()).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }
}
