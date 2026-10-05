package com.minh.fakebook.feed.broker;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.minh.fakebook.feed.service.FriendshipFeedService;
import com.minh.fakebook.feed.service.event.FriendshipUpdatedEvent;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FriendshipEventListenerTest {

    @Test
    void routesCreatedEventToBackfillService() {
        FriendshipFeedService service = mock(FriendshipFeedService.class);
        FriendshipUpdatedEvent event = event("FRIENDSHIP_CREATED", 1);

        new FriendshipEventListener(service).accept(event);

        verify(service).processCreated(event.data().userId(), event.data().friendId());
    }

    @Test
    void routesDeletedEventToCleanupService() {
        FriendshipFeedService service = mock(FriendshipFeedService.class);
        FriendshipUpdatedEvent event = event("FRIENDSHIP_DELETED", 1);

        new FriendshipEventListener(service).accept(event);

        verify(service).processDeleted(event.data().userId(), event.data().friendId());
    }

    @Test
    void rejectsUnsupportedEventVersionForKafkaRetry() {
        FriendshipFeedService service = mock(FriendshipFeedService.class);
        FriendshipEventListener listener = new FriendshipEventListener(service);

        assertThatThrownBy(() -> listener.accept(event("FRIENDSHIP_CREATED", 2)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("version");
    }

    private FriendshipUpdatedEvent event(String type, int version) {
        return new FriendshipUpdatedEvent(
            UUID.randomUUID(),
            type,
            version,
            Instant.parse("2026-09-28T00:00:00Z"),
            new FriendshipUpdatedEvent.Data(UUID.randomUUID(), UUID.randomUUID())
        );
    }
}
