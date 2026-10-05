package com.minh.fakebook.user.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.minh.fakebook.user.service.event.FriendRequestCreatedEvent;
import io.namastack.outbox.Outbox;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FriendRequestRealtimePublisherTest {

    @Test
    void shouldPublishCreatedEventToDedicatedBinding() {
        org.assertj.core.api.Assertions.assertThat(FriendRequestCreatedEvent.class
            .isAnnotationPresent(io.namastack.outbox.annotation.OutboxEvent.class)).isFalse();
        Outbox outbox = mock(Outbox.class);
        FriendRequestCreatedEvent event = FriendRequestCreatedEvent.create(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID()
        );
        new FriendRequestRealtimePublisher(outbox).publish(event);

        verify(outbox).schedule(
            event,
            "friend-request-" + event.data().requestId(),
            Map.of("destination", "friend-request-events")
        );
    }
}
