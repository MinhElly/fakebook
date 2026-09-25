package com.minh.fakebook.user.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.minh.fakebook.user.service.dto.events.FriendRequestCreatedEvent;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.stream.function.StreamBridge;

class FriendRequestRealtimePublisherTest {

    @Test
    void shouldPublishCreatedEventToDedicatedBinding() {
        StreamBridge streamBridge = mock(StreamBridge.class);
        FriendRequestCreatedEvent event = FriendRequestCreatedEvent.create(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID()
        );
        when(streamBridge.send("friendRequestEventsOut-out-0", event)).thenReturn(true);

        new FriendRequestRealtimePublisher(streamBridge).publish(event);

        verify(streamBridge).send("friendRequestEventsOut-out-0", event);
    }
}
