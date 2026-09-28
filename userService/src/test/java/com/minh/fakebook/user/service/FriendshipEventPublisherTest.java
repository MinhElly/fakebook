package com.minh.fakebook.user.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.minh.fakebook.user.service.dto.events.FriendshipUpdatedEvent;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.stream.function.StreamBridge;

class FriendshipEventPublisherTest {

    @Test
    void shouldPublishFriendshipEventToDedicatedBinding() {
        StreamBridge streamBridge = mock(StreamBridge.class);
        FriendshipUpdatedEvent event = FriendshipUpdatedEvent.created(UUID.randomUUID(), UUID.randomUUID());
        when(streamBridge.send("friendshipEventsOut-out-0", event)).thenReturn(true);

        new FriendshipEventPublisher(streamBridge).publish(event);

        verify(streamBridge).send("friendshipEventsOut-out-0", event);
    }
}
