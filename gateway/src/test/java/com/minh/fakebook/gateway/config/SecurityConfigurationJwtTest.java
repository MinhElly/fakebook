package com.minh.fakebook.gateway.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.util.ReflectionTestUtils;
import tech.jhipster.config.JHipsterProperties;

class SecurityConfigurationJwtTest {

    private HttpServer server;
    private RSAKey key;
    private String issuer;
    private ReactiveJwtDecoder decoder;
    private final List<String> userInfoTokens = new ArrayList<>();

    @BeforeEach
    void setUp() throws Exception {
        key = new RSAKeyGenerator(2048).keyID("test-key").generate();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        issuer = "http://127.0.0.1:" + server.getAddress().getPort();
        server.createContext("/jwks", exchange -> {
            byte[] body = new JWKSet(key.toPublicJWK()).toString().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) {
                output.write(body);
            }
        });
        server.createContext("/userinfo", exchange -> {
            userInfoTokens.add(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] body = "{\"sub\":\"same-user\",\"preferred_username\":\"user\",\"name\":\"Test User\"}".getBytes(
                StandardCharsets.UTF_8
            );
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) {
                output.write(body);
            }
        });
        server.start();
        JHipsterProperties properties = new JHipsterProperties();
        properties.getSecurity().getOauth2().setAudience(List.of("web_app"));
        decoder = ReflectionTestUtils.invokeMethod(
            new SecurityConfiguration(null, properties),
            "createJwtDecoder",
            issuer,
            issuer + "/jwks",
            issuer + "/userinfo"
        );
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void preservesEachTokensClaimsWhenEnrichingTheSameUser() throws Exception {
        Instant expiryA = Instant.now().plusSeconds(300).truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
        Instant expiryB = expiryA.plusSeconds(300);
        String tokenA = token(expiryA, "ROLE_ADMIN", issuer, "web_app");
        String tokenB = token(expiryB, "ROLE_USER", issuer, "web_app");

        decoder.decode(tokenA).block(Duration.ofSeconds(10));
        Jwt second = decoder.decode(tokenB).block(Duration.ofSeconds(10));

        assertThat(second).isNotNull();
        assertThat(second.getTokenValue()).isEqualTo(tokenB);
        assertThat(second.getExpiresAt()).isEqualTo(expiryB);
        assertThat(second.getClaimAsStringList("roles")).containsExactly("ROLE_USER");
        assertThat(second.getClaimAsString("given_name")).isEqualTo("Test");
        assertThat(userInfoTokens).containsExactly("Bearer " + tokenA, "Bearer " + tokenB);
    }

    @Test
    void rejectsInvalidTokensBeforeRequestingUserInfo() throws Exception {
        String expired = token(Instant.now().minusSeconds(120), "ROLE_USER", issuer, "web_app");
        String wrongIssuer = token(Instant.now().plusSeconds(300), "ROLE_USER", issuer + "/other", "web_app");
        String wrongAudience = token(Instant.now().plusSeconds(300), "ROLE_USER", issuer, "other");

        for (String invalid : List.of(expired, wrongIssuer, wrongAudience)) {
            assertThatThrownBy(() -> decoder.decode(invalid).block(Duration.ofSeconds(10))).isInstanceOf(JwtException.class);
        }
        assertThat(userInfoTokens).isEmpty();
    }

    @Test
    void doesNotRequestUserInfoForServiceAccountToken() throws Exception {
        Instant expiry = Instant.now().plusSeconds(300).truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
        String token = serviceAccountToken(expiry);

        Jwt decoded = decoder.decode(token).block(Duration.ofSeconds(10));

        assertThat(decoded).isNotNull();
        assertThat(decoded.getClaimAsString("preferred_username")).isEqualTo("service-account-internal");
        assertThat(decoded.getClaimAsStringList("roles")).containsExactly("ROLE_INTERNAL");
        assertThat(userInfoTokens).isEmpty();
    }

    private String token(Instant expiry, String role, String tokenIssuer, String audience) throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
            .subject("same-user")
            .issuer(tokenIssuer)
            .audience(audience)
            .issueTime(Date.from(Instant.now().minusSeconds(600)))
            .expirationTime(Date.from(expiry))
            .claim("roles", List.of(role))
            .build();
        SignedJWT token = new SignedJWT(
            new JWSHeader.Builder(JWSAlgorithm.RS256).type(JOSEObjectType.JWT).keyID(key.getKeyID()).build(),
            claims
        );
        token.sign(new RSASSASigner(key));
        return token.serialize();
    }

    private String serviceAccountToken(Instant expiry) throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
            .subject("service-account-subject")
            .issuer(issuer)
            .audience("web_app")
            .issueTime(Date.from(Instant.now().minusSeconds(60)))
            .expirationTime(Date.from(expiry))
            .claim("preferred_username", "service-account-internal")
            .claim("roles", List.of("ROLE_INTERNAL"))
            .build();
        SignedJWT token = new SignedJWT(
            new JWSHeader.Builder(JWSAlgorithm.RS256).type(JOSEObjectType.JWT).keyID(key.getKeyID()).build(),
            claims
        );
        token.sign(new RSASSASigner(key));
        return token.serialize();
    }
}
