package com.minh.fakebook.gateway.presence;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.scheduling.annotation.EnableScheduling;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

class PresenceExpirySweeperTest {

    private static final Duration WAIT = Duration.ofSeconds(5);

    @Test
    void returnsExpiredSnapshotsAndSkipsUsersRenewedBeforeCleanup() {
        var store = mock(PresenceRedisStore.class);
        UUID expiredUser = UUID.randomUUID();
        UUID renewedUser = UUID.randomUUID();
        Instant observedAt = Instant.parse("2026-10-16T03:00:00Z");
        var expired = new PresenceRedisStore.ExpiryResult(0L, 2L, observedAt);
        when(store.findDueUsers(100)).thenReturn(Flux.just(expiredUser, renewedUser));
        when(store.pruneExpired(expiredUser)).thenReturn(Mono.just(expired));
        when(store.pruneExpired(renewedUser))
            .thenReturn(Mono.just(new PresenceRedisStore.ExpiryResult(1L, 0L, observedAt)));

        assertThat(new PresenceExpirySweeper(store).sweepOnce().collectList().block(WAIT))
            .containsExactly(new PresenceExpirySweeper.UserExpiry(expiredUser, expired));
    }

    @Test
    void continuesOtherUsersWhenOneCleanupFails() {
        var store = mock(PresenceRedisStore.class);
        UUID failedUser = UUID.randomUUID();
        UUID healthyUser = UUID.randomUUID();
        var expired = new PresenceRedisStore.ExpiryResult(0L, 1L, Instant.EPOCH);
        when(store.findDueUsers(100)).thenReturn(Flux.just(failedUser, healthyUser));
        when(store.pruneExpired(failedUser)).thenReturn(Mono.error(new IllegalStateException("Invalid metadata")));
        when(store.pruneExpired(healthyUser)).thenReturn(Mono.just(expired));

        assertThat(new PresenceExpirySweeper(store).sweepOnce().collectList().block(WAIT))
            .containsExactly(new PresenceExpirySweeper.UserExpiry(healthyUser, expired));
    }

    @Test
    void retriesFailedUserOnNextSubscription() {
        var store = mock(PresenceRedisStore.class);
        UUID userId = UUID.randomUUID();
        var expired = new PresenceRedisStore.ExpiryResult(0L, 1L, Instant.EPOCH);
        when(store.findDueUsers(100)).thenReturn(Flux.just(userId));
        when(store.pruneExpired(userId))
            .thenReturn(Mono.error(new IllegalStateException("Temporary failure")), Mono.just(expired));
        var cycle = new PresenceExpirySweeper(store).sweepOnce();

        assertThat(cycle.collectList().block(WAIT)).isEmpty();
        assertThat(cycle.collectList().block(WAIT)).containsExactly(new PresenceExpirySweeper.UserExpiry(userId, expired));
        verify(store, times(2)).findDueUsers(100);
        verify(store, times(2)).pruneExpired(userId);
    }

    @Test
    void scheduledPublisherQueriesRedisAgainOnEverySubscription() {
        var store = mock(PresenceRedisStore.class);
        when(store.findDueUsers(100)).thenReturn(Flux.empty());
        Mono<Void> cycle = new PresenceExpirySweeper(store).sweep();

        cycle.block(WAIT);
        cycle.block(WAIT);

        verify(store, times(2)).findDueUsers(100);
    }

    @Test
    void scheduledPublisherRecoversAfterIndexReadFailure() {
        var store = mock(PresenceRedisStore.class);
        when(store.findDueUsers(100))
            .thenReturn(Flux.error(new IllegalStateException("Redis unavailable")), Flux.empty());
        Mono<Void> cycle = new PresenceExpirySweeper(store).sweep();

        cycle.block(WAIT);
        cycle.block(WAIT);

        verify(store, times(2)).findDueUsers(100);
    }

    @Test
    void springActuallySchedulesRepeatedSweeps() {
        var store = mock(PresenceRedisStore.class);
        var runs = new CountDownLatch(2);
        when(store.findDueUsers(100)).thenAnswer(invocation -> {
            runs.countDown();
            return Flux.empty();
        });

        schedulingContext(store).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(runs.await(3, SECONDS)).isTrue();
        });
    }

    @Test
    void springDoesNotOverlapLongSweepsAndCancelsThemOnShutdown() {
        var store = mock(PresenceRedisStore.class);
        var started = new CountDownLatch(1);
        var overlapped = new CountDownLatch(1);
        var calls = new AtomicInteger();
        var cancelled = new AtomicBoolean();
        when(store.findDueUsers(100)).thenAnswer(invocation -> {
            if (calls.incrementAndGet() > 1) {
                overlapped.countDown();
            }
            started.countDown();
            return Flux.<UUID>never().doOnCancel(() -> cancelled.set(true));
        });

        schedulingContext(store).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(started.await(3, SECONDS)).isTrue();
            assertThat(overlapped.await(200, MILLISECONDS)).isFalse();
        });

        assertThat(cancelled).isTrue();
        assertThat(calls).hasValue(1);
    }

    private ApplicationContextRunner schedulingContext(PresenceRedisStore store) {
        return new ApplicationContextRunner()
            .withPropertyValues("presence.sweep-delay=50ms")
            .withUserConfiguration(SchedulingConfiguration.class)
            .withBean(PresenceRedisStore.class, () -> store)
            .withBean(PresenceExpirySweeper.class);
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableScheduling
    static class SchedulingConfiguration {}
}
