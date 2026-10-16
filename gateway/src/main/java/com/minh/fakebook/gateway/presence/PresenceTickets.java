package com.minh.fakebook.gateway.presence;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import org.springframework.data.redis.core.script.DefaultRedisScript;

@RestController
public class PresenceTickets {
    private final ReactiveStringRedisTemplate redis;
    private final String prefix;

    public PresenceTickets(ReactiveStringRedisTemplate redis,
        @Value("${presence.redis-key-prefix:fakebook:presence}") String prefix) {
        this.redis = redis;
        this.prefix = prefix;
    }

    public record Grant(UUID userId, Instant authExpiresAt) {}
    public record Ticket(String ticket, Instant expiresAt, Instant authExpiresAt) {}

    @PostMapping("/api/presence/tickets")
    public Mono<ResponseEntity<Ticket>> issue(@AuthenticationPrincipal Jwt jwt) {
        return Mono.defer(() -> {
            if (jwt == null || jwt.getExpiresAt() == null || !jwt.getExpiresAt().isAfter(Instant.now())) {
                return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED));
            }
            UUID userId;
            try { userId = UUID.fromString(jwt.getSubject()); }
            catch (IllegalArgumentException error) { return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED)); }
            String ticket = UUID.randomUUID().toString() + UUID.randomUUID();
            Instant expires = Instant.now().plusSeconds(30);
            if (jwt.getExpiresAt().isBefore(expires)) expires = jwt.getExpiresAt();
            Ticket result = new Ticket(ticket, expires, jwt.getExpiresAt());
            return redis.opsForValue().set(prefix + ":ticket:" + ticket,
                userId + "|" + jwt.getExpiresAt().toEpochMilli(), Duration.between(Instant.now(), expires))
                .thenReturn(ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(result));
        });
    }

    private static final DefaultRedisScript<String> CONSUME = new DefaultRedisScript<>(
        "local v = redis.call('GET', KEYS[1]); if v then redis.call('DEL', KEYS[1]) end; return v", String.class);

    public Mono<Grant> consume(String ticket) {
        if (ticket == null || !ticket.matches("[a-f0-9-]{72}")) return Mono.error(new IllegalArgumentException("Invalid ticket"));
        return redis.execute(CONSUME, List.of(prefix + ":ticket:" + ticket)).singleOrEmpty()
            .switchIfEmpty(Mono.error(new IllegalArgumentException("Expired or consumed ticket")))
            .map(value -> {
                String[] parts = value.split("\\|");
                Grant grant = new Grant(UUID.fromString(parts[0]), Instant.ofEpochMilli(Long.parseLong(parts[1])));
                if (!grant.authExpiresAt().isAfter(Instant.now())) throw new IllegalArgumentException("Authentication expired");
                return grant;
            });
    }
}
