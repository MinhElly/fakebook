package com.minh.fakebook.gateway.presence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.stream.LongStream;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration;
import org.springframework.boot.data.redis.autoconfigure.DataRedisReactiveAutoConfiguration;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

class PresenceRedisStoreIT {

    private static final Duration WAIT = Duration.ofSeconds(10);
    private final String keyPrefix = "fakebook:presence:test:" + UUID.randomUUID();
    private final String DUE_KEY = keyPrefix + ":due";

    private final Set<UUID> testUsers = new HashSet<>();

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withInitializer(new ConfigDataApplicationContextInitializer())
        .withPropertyValues(
            "spring.config.location=file:src/main/resources/config/application.yml",
            "spring.profiles.active=presence-redis-test",
            "presence.redis-key-prefix=" + keyPrefix
        )
        .withConfiguration(AutoConfigurations.of(DataRedisAutoConfiguration.class, DataRedisReactiveAutoConfiguration.class))
        .withBean(PresenceRedisStore.class);

    @Test
    void registersConnectionWithLeaseMetadataAndDueIndex() {
        withRedis((redis, store) -> {
            UUID userId = testUser();
            UUID connectionId = UUID.randomUUID();

            assertThat(store.register(userId, connectionId).block(WAIT)).isEqualTo(1L);

            String lastSeen = redis.<String, String>opsForHash()
                .get(metadataKey(userId), "lastSeenAt")
                .block(WAIT);
            assertThat(lastSeen).isNotNull();

            Double expiresAt = redis.opsForZSet()
                .score(connectionsKey(userId), connectionId.toString())
                .block(WAIT);
            assertThat(expiresAt).isEqualTo((double) (Long.parseLong(lastSeen) + 65_000L));
            assertThat(redis.opsForZSet().score(DUE_KEY, userId.toString()).block(WAIT)).isEqualTo(expiresAt);
        });
    }

    @Test
    void countsTabsSeparatelyAndDoesNotDuplicateRepeatedRegistration() {
        withRedis((redis, store) -> {
            UUID userId = testUser();
            UUID firstConnection = UUID.randomUUID();
            UUID secondConnection = UUID.randomUUID();

            assertThat(store.register(userId, firstConnection).block(WAIT)).isEqualTo(1L);
            assertThat(store.register(userId, secondConnection).block(WAIT)).isEqualTo(2L);
            assertThat(store.register(userId, firstConnection).block(WAIT)).isEqualTo(2L);
            assertThat(redis.opsForZSet().size(connectionsKey(userId)).block(WAIT)).isEqualTo(2L);
        });
    }

    @Test
    void keepsDifferentUsersConnectionsSeparate() {
        withRedis((redis, store) -> {
            UUID firstUser = testUser();
            UUID secondUser = testUser();
            UUID connectionId = UUID.randomUUID();

            assertThat(store.register(firstUser, connectionId).block(WAIT)).isEqualTo(1L);
            assertThat(store.register(firstUser, UUID.randomUUID()).block(WAIT)).isEqualTo(2L);
            assertThat(store.register(secondUser, connectionId).block(WAIT)).isEqualTo(1L);
            assertThat(redis.opsForZSet().size(connectionsKey(firstUser)).block(WAIT)).isEqualTo(2L);
            assertThat(redis.opsForZSet().size(connectionsKey(secondUser)).block(WAIT)).isEqualTo(1L);
        });
    }

    @Test
    void registersConcurrentConnectionsWithoutLosingOrDuplicatingCounts() {
        withRedis((redis, store) -> {
            UUID userId = testUser();

            var counts = Flux.range(0, 20)
                .flatMap(index -> store.register(userId, UUID.randomUUID()), 8)
                .collectList()
                .block(WAIT);

            assertThat(counts).containsExactlyInAnyOrderElementsOf(LongStream.rangeClosed(1, 20).boxed().toList());
            assertThat(redis.opsForZSet().size(connectionsKey(userId)).block(WAIT)).isEqualTo(20L);
        });
    }

    @Test
    void removesExpiredConnectionsAndIndexesEarliestRemainingDeadline() {
        withRedis((redis, store) -> {
            UUID userId = testUser();
            UUID expiredConnection = UUID.randomUUID();
            UUID remainingConnection = UUID.randomUUID();
            UUID newConnection = UUID.randomUUID();

            redis.opsForZSet().add(connectionsKey(userId), expiredConnection.toString(), 1D).block(WAIT);
            redis.opsForZSet().add(connectionsKey(userId), remainingConnection.toString(), 4_000_000_000_000D).block(WAIT);

            assertThat(store.register(userId, newConnection).block(WAIT)).isEqualTo(2L);
            assertThat(redis.opsForZSet().score(connectionsKey(userId), expiredConnection.toString()).block(WAIT)).isNull();

            Double firstDeadline = redis.opsForZSet()
                .score(connectionsKey(userId), newConnection.toString())
                .block(WAIT);
            assertThat(firstDeadline).isNotNull();
            assertThat(redis.opsForZSet().score(DUE_KEY, userId.toString()).block(WAIT)).isEqualTo(firstDeadline);
        });
    }

    @Test
    void doesNotMoveLastSeenBackwards() {
        withRedis((redis, store) -> {
            UUID userId = testUser();
            String previousLastSeen = "4000000000000";
            redis.<String, String>opsForHash().put(metadataKey(userId), "lastSeenAt", previousLastSeen).block(WAIT);

            assertThat(store.register(userId, UUID.randomUUID()).block(WAIT)).isEqualTo(1L);
            assertThat(redis.<String, String>opsForHash().get(metadataKey(userId), "lastSeenAt").block(WAIT))
                .isEqualTo(previousLastSeen);
        });
    }

    @Test
    void rejectsWrongMetadataTypeBeforeChangingConnections() {
        withRedis((redis, store) -> {
            UUID userId = testUser();
            redis.opsForValue().set(metadataKey(userId), "wrong-type").block(WAIT);

            assertThatThrownBy(() -> store.register(userId, UUID.randomUUID()).block(WAIT))
                .hasStackTraceContaining("WRONGTYPE");

            assertThat(redis.hasKey(connectionsKey(userId)).block(WAIT)).isFalse();
            assertThat(redis.opsForZSet().score(DUE_KEY, userId.toString()).block(WAIT)).isNull();
            assertThat(redis.opsForValue().get(metadataKey(userId)).block(WAIT)).isEqualTo("wrong-type");
        });
    }

    @Test
    void rejectsInvalidLastSeenBeforeRemovingOrAddingConnections() {
        withRedis((redis, store) -> {
            UUID userId = testUser();
            UUID existingConnection = UUID.randomUUID();
            redis.opsForZSet().add(connectionsKey(userId), existingConnection.toString(), 1D).block(WAIT);
            redis.<String, String>opsForHash().put(metadataKey(userId), "lastSeenAt", "invalid").block(WAIT);

            assertThatThrownBy(() -> store.register(userId, UUID.randomUUID()).block(WAIT))
                .hasStackTraceContaining("Invalid presence lastSeenAt");

            assertThat(redis.opsForZSet().size(connectionsKey(userId)).block(WAIT)).isEqualTo(1L);
            assertThat(redis.opsForZSet().score(connectionsKey(userId), existingConnection.toString()).block(WAIT))
                .isEqualTo(1D);
            assertThat(redis.opsForZSet().score(DUE_KEY, userId.toString()).block(WAIT)).isNull();
        });
    }

    @Test
    void heartbeatRenewsLiveConnectionAndRecordsObservedTime() {
        withRedis((redis, store) -> {
            UUID userId = testUser();
            UUID connectionId = UUID.randomUUID();
            store.register(userId, connectionId).block(WAIT);

            long previousLastSeen = Long.parseLong(lastSeen(redis, userId));
            double previousDeadline = previousLastSeen + 30_000D;
            redis.opsForZSet().add(connectionsKey(userId), connectionId.toString(), previousDeadline).block(WAIT);

            assertThat(store.heartbeat(userId, connectionId).block(WAIT)).isTrue();

            long observedAt = Long.parseLong(lastSeen(redis, userId));
            Double deadline = redis.opsForZSet().score(connectionsKey(userId), connectionId.toString()).block(WAIT);
            assertThat(observedAt).isGreaterThanOrEqualTo(previousLastSeen);
            assertThat(deadline).isEqualTo((double) (observedAt + 65_000L)).isGreaterThan(previousDeadline);
            assertThat(redis.opsForZSet().size(connectionsKey(userId)).block(WAIT)).isEqualTo(1L);
            assertThat(redis.opsForZSet().score(DUE_KEY, userId.toString()).block(WAIT)).isEqualTo(deadline);
        });
    }

    @Test
    void heartbeatDoesNotRegisterUnknownConnection() {
        withRedis((redis, store) -> {
            UUID userId = testUser();

            assertThat(store.heartbeat(userId, UUID.randomUUID()).block(WAIT)).isFalse();

            assertThat(redis.hasKey(connectionsKey(userId)).block(WAIT)).isFalse();
            assertThat(redis.hasKey(metadataKey(userId)).block(WAIT)).isFalse();
            assertThat(redis.opsForZSet().score(DUE_KEY, userId.toString()).block(WAIT)).isNull();
        });
    }

    @Test
    void lateHeartbeatDoesNotReviveExpiredConnectionOrChangeAnotherTab() {
        withRedis((redis, store) -> {
            UUID userId = testUser();
            UUID liveConnection = UUID.randomUUID();
            UUID expiredConnection = UUID.randomUUID();
            store.register(userId, liveConnection).block(WAIT);
            redis.opsForZSet().add(connectionsKey(userId), expiredConnection.toString(), 1D).block(WAIT);

            String observedBefore = lastSeen(redis, userId);
            Double liveDeadline = redis.opsForZSet().score(connectionsKey(userId), liveConnection.toString()).block(WAIT);
            Double dueBefore = redis.opsForZSet().score(DUE_KEY, userId.toString()).block(WAIT);

            assertThat(store.heartbeat(userId, expiredConnection).block(WAIT)).isFalse();

            assertThat(redis.opsForZSet().score(connectionsKey(userId), expiredConnection.toString()).block(WAIT))
                .isEqualTo(1D);
            assertThat(redis.opsForZSet().score(connectionsKey(userId), liveConnection.toString()).block(WAIT))
                .isEqualTo(liveDeadline);
            assertThat(lastSeen(redis, userId)).isEqualTo(observedBefore);
            assertThat(redis.opsForZSet().score(DUE_KEY, userId.toString()).block(WAIT)).isEqualTo(dueBefore);
        });
    }

    @Test
    void heartbeatDoesNotRecreateRemovedConnection() {
        withRedis((redis, store) -> {
            UUID userId = testUser();
            UUID connectionId = UUID.randomUUID();
            store.register(userId, connectionId).block(WAIT);
            String observedBefore = lastSeen(redis, userId);
            Double dueBefore = redis.opsForZSet().score(DUE_KEY, userId.toString()).block(WAIT);
            redis.opsForZSet().remove(connectionsKey(userId), connectionId.toString()).block(WAIT);

            assertThat(store.heartbeat(userId, connectionId).block(WAIT)).isFalse();

            assertThat(redis.hasKey(connectionsKey(userId)).block(WAIT)).isFalse();
            assertThat(lastSeen(redis, userId)).isEqualTo(observedBefore);
            assertThat(redis.opsForZSet().score(DUE_KEY, userId.toString()).block(WAIT)).isEqualTo(dueBefore);
        });
    }

    @Test
    void heartbeatReindexesEarliestDeadlineWithoutRenewingOtherConnections() {
        withRedis((redis, store) -> {
            UUID userId = testUser();
            UUID firstConnection = UUID.randomUUID();
            UUID secondConnection = UUID.randomUUID();
            UUID expiredConnection = UUID.randomUUID();
            store.register(userId, firstConnection).block(WAIT);
            store.register(userId, secondConnection).block(WAIT);
            long observedAt = Long.parseLong(lastSeen(redis, userId));
            double firstDeadline = observedAt + 20_000D;
            double secondDeadline = observedAt + 30_000D;
            redis.opsForZSet().add(connectionsKey(userId), firstConnection.toString(), firstDeadline).block(WAIT);
            redis.opsForZSet().add(connectionsKey(userId), secondConnection.toString(), secondDeadline).block(WAIT);
            redis.opsForZSet().add(connectionsKey(userId), expiredConnection.toString(), 1D).block(WAIT);
            redis.opsForZSet().add(DUE_KEY, userId.toString(), 1D).block(WAIT);

            assertThat(store.heartbeat(userId, firstConnection).block(WAIT)).isTrue();

            assertThat(redis.opsForZSet().score(connectionsKey(userId), firstConnection.toString()).block(WAIT))
                .isGreaterThan(secondDeadline);
            assertThat(redis.opsForZSet().score(connectionsKey(userId), secondConnection.toString()).block(WAIT))
                .isEqualTo(secondDeadline);
            assertThat(redis.opsForZSet().score(connectionsKey(userId), expiredConnection.toString()).block(WAIT)).isNull();
            assertThat(redis.opsForZSet().size(connectionsKey(userId)).block(WAIT)).isEqualTo(2L);
            assertThat(redis.opsForZSet().score(DUE_KEY, userId.toString()).block(WAIT)).isEqualTo(secondDeadline);
        });
    }

    @Test
    void heartbeatDoesNotMoveLastSeenBackwards() {
        withRedis((redis, store) -> {
            UUID userId = testUser();
            UUID connectionId = UUID.randomUUID();
            store.register(userId, connectionId).block(WAIT);
            String previousLastSeen = "4000000000000";
            redis.<String, String>opsForHash().put(metadataKey(userId), "lastSeenAt", previousLastSeen).block(WAIT);

            assertThat(store.heartbeat(userId, connectionId).block(WAIT)).isTrue();

            assertThat(lastSeen(redis, userId)).isEqualTo(previousLastSeen);
        });
    }

    @Test
    void heartbeatRejectsInvalidMetadataBeforeRenewingConnection() {
        withRedis((redis, store) -> {
            UUID userId = testUser();
            UUID connectionId = UUID.randomUUID();
            store.register(userId, connectionId).block(WAIT);
            Double deadlineBefore = redis.opsForZSet().score(connectionsKey(userId), connectionId.toString()).block(WAIT);
            Double dueBefore = redis.opsForZSet().score(DUE_KEY, userId.toString()).block(WAIT);
            redis.<String, String>opsForHash().put(metadataKey(userId), "lastSeenAt", "invalid").block(WAIT);

            assertThatThrownBy(() -> store.heartbeat(userId, connectionId).block(WAIT))
                .hasStackTraceContaining("Invalid presence lastSeenAt");

            assertThat(redis.opsForZSet().score(connectionsKey(userId), connectionId.toString()).block(WAIT))
                .isEqualTo(deadlineBefore);
            assertThat(redis.opsForZSet().score(DUE_KEY, userId.toString()).block(WAIT)).isEqualTo(dueBefore);
            assertThat(lastSeen(redis, userId)).isEqualTo("invalid");
        });
    }

    @Test
    void heartbeatRacingWithRemovalDoesNotResurrectConnection() {
        withRedis((redis, store) -> {
            UUID userId = testUser();
            var connectionIds = LongStream.range(0, 20).mapToObj(index -> UUID.randomUUID()).toList();
            Flux.fromIterable(connectionIds)
                .flatMap(connectionId -> store.register(userId, connectionId), 8)
                .then()
                .block(WAIT);

            Flux.fromIterable(connectionIds)
                .flatMap(connectionId -> Mono.when(
                    store.heartbeat(userId, connectionId),
                    redis.opsForZSet().remove(connectionsKey(userId), connectionId.toString())
                ), 8)
                .then()
                .block(WAIT);

            assertThat(redis.opsForZSet().size(connectionsKey(userId)).block(WAIT)).isZero();
        });
    }

    @Test
    void disconnectKeepsAnotherTabLiveAndReindexesItsDeadline() {
        withRedis((redis, store) -> {
            UUID userId = testUser();
            UUID firstConnection = UUID.randomUUID();
            UUID secondConnection = UUID.randomUUID();
            store.register(userId, firstConnection).block(WAIT);
            store.register(userId, secondConnection).block(WAIT);
            Instant observedAt = Instant.ofEpochMilli(Long.parseLong(lastSeen(redis, userId)));
            Double remainingDeadline = redis.opsForZSet().score(connectionsKey(userId), secondConnection.toString()).block(WAIT);

            assertThat(store.disconnect(userId, firstConnection).block(WAIT))
                .isEqualTo(new PresenceRedisStore.DisconnectResult(1L, true, observedAt));
            assertThat(store.disconnect(userId, firstConnection).block(WAIT))
                .isEqualTo(new PresenceRedisStore.DisconnectResult(1L, false, observedAt));

            assertThat(redis.opsForZSet().score(connectionsKey(userId), firstConnection.toString()).block(WAIT)).isNull();
            assertThat(redis.opsForZSet().score(connectionsKey(userId), secondConnection.toString()).block(WAIT))
                .isEqualTo(remainingDeadline);
            assertThat(redis.opsForZSet().score(DUE_KEY, userId.toString()).block(WAIT)).isEqualTo(remainingDeadline);
            assertThat(store.heartbeat(userId, firstConnection).block(WAIT)).isFalse();
        });
    }

    @Test
    void finalDisconnectReturnsLastObservedTimeAndRetainsItForPersistence() {
        withRedis((redis, store) -> {
            UUID userId = testUser();
            UUID connectionId = UUID.randomUUID();
            store.register(userId, connectionId).block(WAIT);
            String observedAt = lastSeen(redis, userId);

            assertThat(store.disconnect(userId, connectionId).block(WAIT))
                .isEqualTo(new PresenceRedisStore.DisconnectResult(0L, true, Instant.ofEpochMilli(Long.parseLong(observedAt))));

            assertThat(redis.hasKey(connectionsKey(userId)).block(WAIT)).isFalse();
            assertThat(redis.opsForZSet().score(DUE_KEY, userId.toString()).block(WAIT)).isNull();
            assertThat(lastSeen(redis, userId)).isEqualTo(observedAt);
        });
    }

    @Test
    void repeatedDisconnectDoesNotRecreateConnectionsOrLoseLastSeen() {
        withRedis((redis, store) -> {
            UUID userId = testUser();
            UUID connectionId = UUID.randomUUID();
            store.register(userId, connectionId).block(WAIT);
            Instant observedAt = Instant.ofEpochMilli(Long.parseLong(lastSeen(redis, userId)));
            store.disconnect(userId, connectionId).block(WAIT);

            assertThat(store.disconnect(userId, connectionId).block(WAIT))
                .isEqualTo(new PresenceRedisStore.DisconnectResult(0L, false, observedAt));
            assertThat(redis.hasKey(connectionsKey(userId)).block(WAIT)).isFalse();
            assertThat(redis.opsForZSet().score(DUE_KEY, userId.toString()).block(WAIT)).isNull();
        });
    }

    @Test
    void disconnectOfUnknownUserDoesNotInventLastSeenOrCreateKeys() {
        withRedis((redis, store) -> {
            UUID userId = testUser();

            assertThat(store.disconnect(userId, UUID.randomUUID()).block(WAIT))
                .isEqualTo(new PresenceRedisStore.DisconnectResult(0L, false, null));

            assertThat(redis.hasKey(connectionsKey(userId)).block(WAIT)).isFalse();
            assertThat(redis.hasKey(metadataKey(userId)).block(WAIT)).isFalse();
            assertThat(redis.opsForZSet().score(DUE_KEY, userId.toString()).block(WAIT)).isNull();
        });
    }

    @Test
    void disconnectPrunesExpiredTabsWithoutAdvancingLastSeenToCleanupTime() {
        withRedis((redis, store) -> {
            UUID userId = testUser();
            UUID connectionId = UUID.randomUUID();
            UUID expiredConnection = UUID.randomUUID();
            store.register(userId, connectionId).block(WAIT);
            long observedBeforeClose = Long.parseLong(lastSeen(redis, userId)) - 1_000L;
            redis.<String, String>opsForHash()
                .put(metadataKey(userId), "lastSeenAt", Long.toString(observedBeforeClose))
                .block(WAIT);
            redis.opsForZSet().add(connectionsKey(userId), expiredConnection.toString(), 1D).block(WAIT);

            assertThat(store.disconnect(userId, connectionId).block(WAIT))
                .isEqualTo(new PresenceRedisStore.DisconnectResult(0L, true, Instant.ofEpochMilli(observedBeforeClose)));

            assertThat(redis.hasKey(connectionsKey(userId)).block(WAIT)).isFalse();
            assertThat(redis.opsForZSet().score(DUE_KEY, userId.toString()).block(WAIT)).isNull();
            assertThat(lastSeen(redis, userId)).isEqualTo(Long.toString(observedBeforeClose));
        });
    }

    @Test
    void concurrentDisconnectsReturnConsistentRemainingCounts() {
        withRedis((redis, store) -> {
            UUID userId = testUser();
            var connectionIds = LongStream.range(0, 20).mapToObj(index -> UUID.randomUUID()).toList();
            Flux.fromIterable(connectionIds)
                .flatMap(connectionId -> store.register(userId, connectionId), 8)
                .then()
                .block(WAIT);
            Instant observedAt = Instant.ofEpochMilli(Long.parseLong(lastSeen(redis, userId)));

            var results = Flux.fromIterable(connectionIds)
                .flatMap(connectionId -> store.disconnect(userId, connectionId), 8)
                .collectList()
                .block(WAIT);

            assertThat(results).hasSize(20).allSatisfy(result -> {
                assertThat(result.removed()).isTrue();
                assertThat(result.lastSeenAt()).isEqualTo(observedAt);
            });
            assertThat(results).extracting(PresenceRedisStore.DisconnectResult::remainingConnections)
                .containsExactlyInAnyOrderElementsOf(LongStream.range(0, 20).boxed().toList());
            assertThat(redis.hasKey(connectionsKey(userId)).block(WAIT)).isFalse();
            assertThat(redis.opsForZSet().score(DUE_KEY, userId.toString()).block(WAIT)).isNull();
        });
    }

    @Test
    void heartbeatRacingWithDisconnectLeavesNoConnectionOrDueEntry() {
        withRedis((redis, store) -> {
            UUID userId = testUser();
            var connectionIds = LongStream.range(0, 20).mapToObj(index -> UUID.randomUUID()).toList();
            Flux.fromIterable(connectionIds)
                .flatMap(connectionId -> store.register(userId, connectionId), 8)
                .then()
                .block(WAIT);

            Flux.fromIterable(connectionIds)
                .flatMap(connectionId -> Mono.when(
                    store.heartbeat(userId, connectionId),
                    store.disconnect(userId, connectionId)
                ), 8)
                .then()
                .block(WAIT);

            assertThat(redis.hasKey(connectionsKey(userId)).block(WAIT)).isFalse();
            assertThat(redis.opsForZSet().score(DUE_KEY, userId.toString()).block(WAIT)).isNull();
            assertThat(store.heartbeat(userId, connectionIds.getFirst()).block(WAIT)).isFalse();
        });
    }

    @Test
    void disconnectRejectsInvalidMetadataBeforeRemovingAnyConnection() {
        withRedis((redis, store) -> {
            UUID userId = testUser();
            UUID connectionId = UUID.randomUUID();
            UUID expiredConnection = UUID.randomUUID();
            store.register(userId, connectionId).block(WAIT);
            redis.opsForZSet().add(connectionsKey(userId), expiredConnection.toString(), 1D).block(WAIT);
            Double liveDeadline = redis.opsForZSet().score(connectionsKey(userId), connectionId.toString()).block(WAIT);
            Double dueBefore = redis.opsForZSet().score(DUE_KEY, userId.toString()).block(WAIT);
            redis.<String, String>opsForHash().put(metadataKey(userId), "lastSeenAt", "invalid").block(WAIT);

            assertThatThrownBy(() -> store.disconnect(userId, connectionId).block(WAIT))
                .hasStackTraceContaining("Invalid presence lastSeenAt");

            assertThat(redis.opsForZSet().size(connectionsKey(userId)).block(WAIT)).isEqualTo(2L);
            assertThat(redis.opsForZSet().score(connectionsKey(userId), connectionId.toString()).block(WAIT))
                .isEqualTo(liveDeadline);
            assertThat(redis.opsForZSet().score(connectionsKey(userId), expiredConnection.toString()).block(WAIT))
                .isEqualTo(1D);
            assertThat(redis.opsForZSet().score(DUE_KEY, userId.toString()).block(WAIT)).isEqualTo(dueBefore);
        });
    }

    @Test
    void findsOnlyDueUsersInBoundedDeadlineOrderWithoutRemovingEntries() {
        withRedis((redis, store) -> {
            UUID firstUser = testUser();
            UUID secondUser = testUser();
            UUID thirdUser = testUser();
            UUID futureUser = testUser();
            redis.opsForZSet().add(DUE_KEY, thirdUser.toString(), 3D).block(WAIT);
            redis.opsForZSet().add(DUE_KEY, firstUser.toString(), 1D).block(WAIT);
            redis.opsForZSet().add(DUE_KEY, secondUser.toString(), 2D).block(WAIT);
            redis.opsForZSet().add(DUE_KEY, futureUser.toString(), 4_000_000_000_000D).block(WAIT);

            assertThat(store.findDueUsers(2).collectList().block(WAIT)).containsExactly(firstUser, secondUser);
            assertThat(redis.opsForZSet().size(DUE_KEY).block(WAIT)).isEqualTo(4L);
        });
    }

    @Test
    void findsNoUsersWhenDueIndexIsEmpty() {
        withRedis((redis, store) -> {
            assertThat(store.findDueUsers(100).collectList().block(WAIT)).isEmpty();
            assertThat(redis.hasKey(DUE_KEY).block(WAIT)).isFalse();
        });
    }

    @Test
    void expiryRemovesDeadConnectionsAndKeepsActualLastSeenOnRepeatedCleanup() {
        withRedis((redis, store) -> {
            UUID userId = testUser();
            UUID connectionId = UUID.randomUUID();
            store.register(userId, connectionId).block(WAIT);
            long observedAt = Long.parseLong(lastSeen(redis, userId)) - 1_000L;
            redis.<String, String>opsForHash().put(metadataKey(userId), "lastSeenAt", Long.toString(observedAt)).block(WAIT);
            redis.opsForZSet().add(connectionsKey(userId), connectionId.toString(), 1D).block(WAIT);
            redis.opsForZSet().add(DUE_KEY, userId.toString(), 1D).block(WAIT);

            assertThat(store.pruneExpired(userId).block(WAIT))
                .isEqualTo(new PresenceRedisStore.ExpiryResult(0L, 1L, Instant.ofEpochMilli(observedAt)));
            assertThat(store.pruneExpired(userId).block(WAIT))
                .isEqualTo(new PresenceRedisStore.ExpiryResult(0L, 0L, Instant.ofEpochMilli(observedAt)));
            assertThat(redis.hasKey(connectionsKey(userId)).block(WAIT)).isFalse();
            assertThat(redis.opsForZSet().score(DUE_KEY, userId.toString()).block(WAIT)).isNull();
            assertThat(lastSeen(redis, userId)).isEqualTo(Long.toString(observedAt));
        });
    }

    @Test
    void expiryRechecksHeartbeatReceivedAfterReadingDueBatch() {
        withRedis((redis, store) -> {
            UUID userId = testUser();
            UUID liveConnection = UUID.randomUUID();
            UUID expiredConnection = UUID.randomUUID();
            store.register(userId, liveConnection).block(WAIT);
            redis.opsForZSet().add(connectionsKey(userId), expiredConnection.toString(), 1D).block(WAIT);
            redis.opsForZSet().add(DUE_KEY, userId.toString(), 1D).block(WAIT);
            assertThat(store.findDueUsers(100).collectList().block(WAIT)).containsExactly(userId);

            assertThat(store.heartbeat(userId, liveConnection).block(WAIT)).isTrue();
            Instant lastPong = Instant.ofEpochMilli(Long.parseLong(lastSeen(redis, userId)));

            assertThat(store.pruneExpired(userId).block(WAIT))
                .isEqualTo(new PresenceRedisStore.ExpiryResult(1L, 0L, lastPong));
            Double liveDeadline = redis.opsForZSet().score(connectionsKey(userId), liveConnection.toString()).block(WAIT);
            assertThat(liveDeadline).isNotNull();
            assertThat(redis.opsForZSet().score(DUE_KEY, userId.toString()).block(WAIT)).isEqualTo(liveDeadline);
            assertThat(store.findDueUsers(100).collectList().block(WAIT)).isEmpty();
        });
    }

    @Test
    void sweepHandlesOfflineUsersLiveTabsAndStaleIndexEntries() {
        withRedis((redis, store) -> {
            UUID offlineUser = testUser();
            UUID onlineUser = testUser();
            UUID staleUser = testUser();
            UUID offlineConnection = UUID.randomUUID();
            UUID onlineConnection = UUID.randomUUID();
            UUID expiredTab = UUID.randomUUID();
            store.register(offlineUser, offlineConnection).block(WAIT);
            store.register(onlineUser, onlineConnection).block(WAIT);
            Instant offlineLastSeen = Instant.ofEpochMilli(Long.parseLong(lastSeen(redis, offlineUser)));
            Instant onlineLastSeen = Instant.ofEpochMilli(Long.parseLong(lastSeen(redis, onlineUser)));
            redis.opsForZSet().add(connectionsKey(offlineUser), offlineConnection.toString(), 1D).block(WAIT);
            redis.opsForZSet().add(connectionsKey(onlineUser), expiredTab.toString(), 1D).block(WAIT);
            redis.opsForZSet().add(DUE_KEY, offlineUser.toString(), 1D).block(WAIT);
            redis.opsForZSet().add(DUE_KEY, onlineUser.toString(), 1D).block(WAIT);
            redis.opsForZSet().add(DUE_KEY, staleUser.toString(), 1D).block(WAIT);

            var results = new PresenceExpirySweeper(store).sweepOnce().collectList().block(WAIT);

            assertThat(results).containsExactlyInAnyOrder(
                new PresenceExpirySweeper.UserExpiry(offlineUser, new PresenceRedisStore.ExpiryResult(0L, 1L, offlineLastSeen)),
                new PresenceExpirySweeper.UserExpiry(onlineUser, new PresenceRedisStore.ExpiryResult(1L, 1L, onlineLastSeen))
            );
            assertThat(redis.hasKey(connectionsKey(offlineUser)).block(WAIT)).isFalse();
            assertThat(redis.opsForZSet().size(connectionsKey(onlineUser)).block(WAIT)).isEqualTo(1L);
            assertThat(redis.opsForZSet().score(DUE_KEY, offlineUser.toString()).block(WAIT)).isNull();
            assertThat(redis.opsForZSet().score(DUE_KEY, staleUser.toString()).block(WAIT)).isNull();
            assertThat(redis.hasKey(metadataKey(staleUser)).block(WAIT)).isFalse();
        });
    }

    @Test
    void concurrentSweepersOnlyReportEachExpiredConnectionOnce() {
        withRedis((redis, store) -> {
            UUID userId = testUser();
            UUID connectionId = UUID.randomUUID();
            store.register(userId, connectionId).block(WAIT);
            redis.opsForZSet().add(connectionsKey(userId), connectionId.toString(), 1D).block(WAIT);
            redis.opsForZSet().add(DUE_KEY, userId.toString(), 1D).block(WAIT);

            var results = Mono.zip(store.pruneExpired(userId), store.pruneExpired(userId)).block(WAIT);

            assertThat(results).isNotNull();
            assertThat(results.getT1().expiredConnections() + results.getT2().expiredConnections()).isEqualTo(1L);
            assertThat(results.getT1().remainingConnections()).isZero();
            assertThat(results.getT2().remainingConnections()).isZero();
            assertThat(results.getT1().lastSeenAt()).isEqualTo(results.getT2().lastSeenAt());
            assertThat(redis.opsForZSet().score(DUE_KEY, userId.toString()).block(WAIT)).isNull();
        });
    }

    @Test
    void failedExpiryKeepsDueEntryForRetryAfterMetadataIsRepaired() {
        withRedis((redis, store) -> {
            UUID userId = testUser();
            UUID connectionId = UUID.randomUUID();
            store.register(userId, connectionId).block(WAIT);
            String actualLastSeen = lastSeen(redis, userId);
            redis.opsForZSet().add(connectionsKey(userId), connectionId.toString(), 1D).block(WAIT);
            redis.opsForZSet().add(DUE_KEY, userId.toString(), 1D).block(WAIT);
            redis.<String, String>opsForHash().put(metadataKey(userId), "lastSeenAt", "invalid").block(WAIT);

            assertThatThrownBy(() -> store.pruneExpired(userId).block(WAIT)).hasStackTraceContaining("Invalid presence lastSeenAt");
            assertThat(store.findDueUsers(100).collectList().block(WAIT)).containsExactly(userId);
            assertThat(redis.opsForZSet().score(connectionsKey(userId), connectionId.toString()).block(WAIT)).isEqualTo(1D);

            redis.<String, String>opsForHash().put(metadataKey(userId), "lastSeenAt", actualLastSeen).block(WAIT);
            assertThat(store.pruneExpired(userId).block(WAIT).expiredConnections()).isEqualTo(1L);
            assertThat(store.findDueUsers(100).collectList().block(WAIT)).isEmpty();
        });
    }

    @Test
    void queuesCheckpointAndRetainsNewerObservationDuringAcknowledgement() {
        withRedis((redis, store) -> {
            UUID user = testUser(); UUID connection = UUID.randomUUID();
            store.register(user, connection).block(WAIT);
            assertThat(store.findPending(100).collectList().block(WAIT)).contains(user);
            Instant claimed = store.claimPending(user, "worker-a").block(WAIT);
            assertThat(store.claimPending(user, "worker-b").block(WAIT)).isNull();
            // A PONG or final close can race an in-flight HTTP checkpoint.
            redis.<String, String>opsForHash().put(keyPrefix + ":pending", user.toString(),
                Long.toString(claimed.toEpochMilli() + 1)).block(WAIT);
            store.finishPending(user, "worker-a", claimed, true).block(WAIT);
            assertThat(redis.<String, String>opsForHash().get(keyPrefix + ":pending", user.toString()).block(WAIT))
                .isEqualTo(Long.toString(claimed.toEpochMilli() + 1));
            assertThat(store.findPending(100).collectList().block(WAIT)).doesNotContain(user);
            store.disconnect(user, connection).block(WAIT);
            assertThat(store.findPending(100).collectList().block(WAIT)).contains(user);
        });
    }

    @Test
    void failedWriteKeepsPendingAndStaleWorkerCannotAcknowledgeAnotherLease() {
        withRedis((redis, store) -> {
            UUID user = testUser();
            store.register(user, UUID.randomUUID()).block(WAIT);
            Instant time = store.claimPending(user, "owner").block(WAIT);
            store.finishPending(user, "stale-owner", time, true).block(WAIT);
            assertThat(redis.<String, String>opsForHash().get(keyPrefix + ":pending", user.toString()).block(WAIT)).isNotNull();
            store.finishPending(user, "owner", time, false).block(WAIT);
            assertThat(redis.<String, String>opsForHash().get(keyPrefix + ":pending", user.toString()).block(WAIT)).isNotNull();
            assertThat(redis.hasKey(keyPrefix + ":persist-lock:" + user).block(WAIT)).isFalse();
        });
    }

    @Test
    void offlineAcknowledgementExpiresMetadataButRegisterMakesItPersistentAgain() {
        withRedis((redis, store) -> {
            UUID user = testUser(); UUID connection = UUID.randomUUID();
            store.register(user, connection).block(WAIT);
            store.disconnect(user, connection).block(WAIT);
            Instant time = store.claimPending(user, "owner").block(WAIT);
            store.finishPending(user, "owner", time, true).block(WAIT);
            assertThat(store.findPending(100).collectList().block(WAIT)).doesNotContain(user);
            assertThat(redis.getExpire(metadataKey(user)).block(WAIT)).isPositive();
            store.register(user, connection).block(WAIT);
            assertThat(redis.execute(new org.springframework.data.redis.core.script.DefaultRedisScript<>(
                "return redis.call('PTTL', KEYS[1])", Long.class), java.util.List.of(metadataKey(user))).single().block(WAIT)).isEqualTo(-1L);
            assertThat(store.snapshot(user).block(WAIT).online()).isTrue();
        });
    }

    private String lastSeen(ReactiveStringRedisTemplate redis, UUID userId) {
        return redis.<String, String>opsForHash().get(metadataKey(userId), "lastSeenAt").block(WAIT);
    }

    private void withRedis(BiConsumer<ReactiveStringRedisTemplate, PresenceRedisStore> test) {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            var redis = context.getBean(ReactiveStringRedisTemplate.class);
            var store = context.getBean(PresenceRedisStore.class);

            try {
                test.accept(redis, store);
            } finally {
                // Only remove this test's UUID keys and members, never the shared index.
                for (UUID userId : testUsers) {
                    redis.delete(connectionsKey(userId), metadataKey(userId), keyPrefix + ":persist-lock:" + userId).block(WAIT);
                    redis.opsForZSet().remove(DUE_KEY, userId.toString()).block(WAIT);
                }
                redis.delete(keyPrefix + ":pending", keyPrefix + ":persist-due").block(WAIT);
            }
        });
    }

    private UUID testUser() {
        UUID userId = UUID.randomUUID();
        testUsers.add(userId);
        return userId;
    }

    private String connectionsKey(UUID userId) {
        return keyPrefix + ":user:" + userId + ":connections";
    }

    private String metadataKey(UUID userId) {
        return keyPrefix + ":user:" + userId + ":metadata";
    }
}
