package com.minh.fakebook.user.service;

import com.minh.fakebook.user.service.event.FriendRequestCreatedEvent;
import io.namastack.outbox.Outbox;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class FriendRequestRealtimePublisher {

    private static final String DESTINATION = "friend-request-events";

    private final Outbox outbox;

    public FriendRequestRealtimePublisher(Outbox outbox) {
        this.outbox = outbox;
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void publish(FriendRequestCreatedEvent event) {
        outbox.schedule(event, "friend-request-" + event.data().requestId(), Map.of("destination", DESTINATION));
    }
}
