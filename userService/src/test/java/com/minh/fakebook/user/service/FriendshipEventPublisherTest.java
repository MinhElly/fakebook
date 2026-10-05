package com.minh.fakebook.user.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.minh.fakebook.user.service.event.FriendshipUpdatedEvent;
import io.namastack.outbox.Outbox;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FriendshipEventPublisherTest {

    @Test
    void shouldPublishFriendshipEventToDedicatedBinding() {
        org.assertj.core.api.Assertions.assertThat(FriendshipUpdatedEvent.class
            .isAnnotationPresent(io.namastack.outbox.annotation.OutboxEvent.class)).isFalse();
        Outbox outbox = mock(Outbox.class);
        FriendshipUpdatedEvent event = FriendshipUpdatedEvent.created(UUID.randomUUID(), UUID.randomUUID());

        new FriendshipEventPublisher(outbox).publish(event);

        verify(outbox).schedule(event, "friendship-" + event.data().userId(), Map.of("destination", "friendship-events"));
    }
}
