package com.minh.fakebook.user.service;

import com.minh.fakebook.user.service.dto.events.FriendRequestCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class FriendRequestRealtimePublisher {

    private static final Logger LOG = LoggerFactory.getLogger(FriendRequestRealtimePublisher.class);
    private static final String BINDING_NAME = "friendRequestEventsOut-out-0";

    private final StreamBridge streamBridge;

    public FriendRequestRealtimePublisher(StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(FriendRequestCreatedEvent event) {
        if (!streamBridge.send(BINDING_NAME, event)) {
            LOG.warn("Could not publish realtime event for friend request {}", event.data().requestId());
        }
    }
}
