package com.minh.fakebook.media.security;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

/** Opaque, media-scoped leases let native video make authorized Range requests. Never expose JWTs in URLs. */
@Component
public class PlaybackTickets {
    private record Lease(UUID mediaId, String owner, String bearer, Instant expiresAt) {}
    private final Map<String, Lease> leases = new ConcurrentHashMap<>();

    public synchronized String issue(UUID id, JwtAuthenticationToken auth, String previous) {
        Instant now = Instant.now();
        leases.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(now));
        Instant expiresAt = auth.getToken().getExpiresAt();
        if (expiresAt == null || !expiresAt.isAfter(now)) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        Instant deadline = expiresAt.isBefore(now.plusSeconds(300)) ? expiresAt : now.plusSeconds(300);
        Lease old = previous == null ? null : leases.get(previous);
        String key;
        if (old != null && old.mediaId().equals(id) && old.owner().equals(auth.getName())) key = previous;
        else {
            if (leases.size() >= 10000) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Playback capacity reached");
            key = UUID.randomUUID().toString() + UUID.randomUUID();
        }
        leases.put(key, new Lease(id, auth.getName(), auth.getToken().getTokenValue(), deadline));
        return key;
    }

    public String bearer(UUID id, String ticket) {
        Lease lease = ticket == null ? null : leases.get(ticket);
        if (lease == null || !lease.mediaId().equals(id)) return null;
        if (!lease.expiresAt().isAfter(Instant.now())) { leases.remove(ticket, lease); return null; }
        return lease.bearer();
    }
}
