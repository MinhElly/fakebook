package com.minh.fakebook.gateway.presence;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class PresenceExpirySweeper {

    private static final Logger LOG = LoggerFactory.getLogger(PresenceExpirySweeper.class);
    private static final int BATCH_SIZE = 100;
    private static final int CONCURRENCY = 8;

    private final PresenceRedisStore store;

    public PresenceExpirySweeper(PresenceRedisStore store) {
        this.store = store;
    }

    public record UserExpiry(UUID userId, PresenceRedisStore.ExpiryResult state) {}

    /** The deferred publisher is subscribed afresh on every scheduled run. */
    @Scheduled(
        fixedDelayString = "${presence.sweep-delay:5s}",
        initialDelayString = "${presence.sweep-delay:5s}"
    )
    public Mono<Void> sweep() {
        return Mono.defer(() -> sweepOnce().then())
            .onErrorResume(error -> {
                LOG.warn("Presence expiry sweep failed; will retry on the next run", error);
                return Mono.empty();
            });
    }

    /** Cleanup snapshots can be composed with persistence and realtime processing. */
    public Flux<UserExpiry> sweepOnce() {
        return Flux.defer(() -> store.findDueUsers(BATCH_SIZE))
            .flatMap(userId -> store.pruneExpired(userId)
                .filter(state -> state.expiredConnections() > 0)
                .map(state -> new UserExpiry(userId, state))
                .onErrorResume(error -> {
                    // Validation failures leave the due entry available for retry.
                    LOG.warn("Could not expire presence for user {}; will retry", userId, error);
                    return Mono.empty();
                }), CONCURRENCY);
    }
}
