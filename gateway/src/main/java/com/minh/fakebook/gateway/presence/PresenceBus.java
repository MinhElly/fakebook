package com.minh.fakebook.gateway.presence;

import java.time.Duration;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

/** Invalidation hints only. Every recipient re-reads DB authorization and Redis state. */
@Component
public class PresenceBus {
    private final ReactiveStringRedisTemplate redis;
    private final PresenceRedisStore store;
    private final Flux<String> changes;

    public PresenceBus(ReactiveStringRedisTemplate redis, PresenceRedisStore store) {
        this.redis = redis;
        this.store = store;
        this.changes = Flux.defer(() -> redis.listenToChannel(store.channel()).map(message -> message.getMessage()))
            .retryWhen(Retry.backoff(Long.MAX_VALUE, Duration.ofSeconds(1)).maxBackoff(Duration.ofSeconds(30)))
            .share();
    }

    public Flux<String> changes() { return changes; }
    public Mono<Void> invalidateFriendships() { return redis.convertAndSend(store.channel(), "*").then(); }
}
