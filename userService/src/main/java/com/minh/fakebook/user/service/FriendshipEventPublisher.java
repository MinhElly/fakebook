package com.minh.fakebook.user.service;

import com.minh.fakebook.user.service.dto.events.FriendshipUpdatedEvent;
import io.namastack.outbox.Outbox;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class FriendshipEventPublisher {
    private static final String DESTINATION = "friendship-events";
    private final Outbox outbox;

    public FriendshipEventPublisher(Outbox outbox) {
        this.outbox = outbox;
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void publish(FriendshipUpdatedEvent event) {
        outbox.schedule(event, "friendship-" + event.data().userId(), Map.of("destination", DESTINATION));
    }
}
