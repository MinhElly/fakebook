package com.minh.fakebook.gateway.presence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.minh.fakebook.gateway.client.UserPresenceClient;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

class PresencePersistenceWorkerTest {
    @Test
    void acknowledgesOnlySuccessfulWritesAndKeepsFailedWritesForRetry() {
        var store = mock(PresenceRedisStore.class); var users = mock(UserPresenceClient.class);
        UUID success = UUID.randomUUID(), failure = UUID.randomUUID();
        Instant observation = Instant.parse("2026-10-16T00:00:00Z");
        when(store.findPending(100)).thenReturn(Flux.just(success, failure));
        when(store.claimPending(any(), anyString())).thenReturn(Mono.just(observation));
        when(users.recordLastSeen(success, observation)).thenReturn(Mono.empty());
        when(users.recordLastSeen(failure, observation)).thenReturn(Mono.error(new IllegalStateException("Unavailable")));
        when(store.finishPending(any(), anyString(), any(), anyBoolean())).thenReturn(Mono.empty());
        new PresencePersistenceWorker(store, users).persist().block();
        verify(store).finishPending(eq(success), anyString(), eq(observation), eq(true));
        verify(store).finishPending(eq(failure), anyString(), eq(observation), eq(false));
    }

    @Test
    void doesNotWriteAnObservationClaimedByAnotherGateway() {
        var store = mock(PresenceRedisStore.class); var users = mock(UserPresenceClient.class);
        when(store.findPending(100)).thenReturn(Flux.just(UUID.randomUUID()));
        when(store.claimPending(any(), anyString())).thenReturn(Mono.empty());
        new PresencePersistenceWorker(store, users).persist().block();
        verifyNoInteractions(users);
    }

    @Test
    void retriesFreshQueueAfterRedisOutage() {
        var store = mock(PresenceRedisStore.class); var users = mock(UserPresenceClient.class);
        when(store.findPending(100)).thenReturn(Flux.error(new IllegalStateException("Redis down")), Flux.empty());
        var run = new PresencePersistenceWorker(store, users).persist();
        assertThat(run.block()).isNull(); assertThat(run.block()).isNull();
        verify(store, times(2)).findPending(100);
    }
}
