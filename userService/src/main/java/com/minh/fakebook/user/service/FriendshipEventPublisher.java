package com.minh.fakebook.user.service;

import com.minh.fakebook.user.service.dto.events.FriendshipUpdatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class FriendshipEventPublisher {
    private static final Logger LOGGER = LoggerFactory.getLogger(FriendshipEventPublisher.class);
    private static final String BINDING_NAME = "friendshipEventsOut-out-0";
    private final StreamBridge streamBridge;

    public FriendshipEventPublisher(StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
    }
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(FriendshipUpdatedEvent event) {
        boolean sent = streamBridge.send(BINDING_NAME, event);
        if(!sent) {
            LOGGER.warn("Could not publish {} event {} between users {} and {}",
                event.eventType(),
                event.eventId(),
                event.data().userId(),
                event.data().friendId());
        }
    }
}
