package com.minh.fakebook.gateway.presence;

import com.minh.fakebook.gateway.client.UserPresenceClient;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class PresencePersistenceWorker {
    private static final Logger LOG = LoggerFactory.getLogger(PresencePersistenceWorker.class);
    private final PresenceRedisStore store;
    private final UserPresenceClient client;

    public PresencePersistenceWorker(PresenceRedisStore store, UserPresenceClient client) {
        this.store = store;
        this.client = client;
    }

    @Scheduled(fixedDelayString = "${presence.persist-delay:1s}")
    public Mono<Void> persist() {
        return Mono.defer(() -> store.findPending(100).flatMap(userId -> {
            String owner = UUID.randomUUID().toString();
            return store.claimPending(userId, owner).flatMap(observation ->
                client.recordLastSeen(userId, observation)
                    .then(store.finishPending(userId, owner, observation, true))
                    .onErrorResume(error -> {
                        LOG.warn("Presence checkpoint failed for {}; retry remains in Redis", userId);
                        return store.finishPending(userId, owner, observation, false);
                    })
            ).onErrorResume(error -> Mono.empty());
        }, 8).then()).onErrorResume(error -> {
            LOG.warn("Presence persistence queue unavailable; will retry");
            return Mono.empty();
        });
    }
}
