package com.minh.fakebook.feed.broker;

import com.minh.fakebook.feed.service.FriendshipFeedService;
import com.minh.fakebook.feed.service.event.FriendshipUpdatedEvent;
import java.util.UUID;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("processFriendshipEvent")
public class FriendshipEventListener implements Consumer<FriendshipUpdatedEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(FriendshipEventListener.class);
    private static final int SUPPORTED_EVENT_VERSION = 1;

    private final FriendshipFeedService friendshipFeedService;

    public FriendshipEventListener(FriendshipFeedService friendshipFeedService) {
        this.friendshipFeedService = friendshipFeedService;
    }

    @Override
    public void accept(FriendshipUpdatedEvent event) {
        validateEnvelope(event);
        UUID userId = event.data().userId();
        UUID friendId = event.data().friendId();
        switch (event.eventType()) {
            case "FRIENDSHIP_CREATED" -> friendshipFeedService.processCreated(userId, friendId);
            case "FRIENDSHIP_DELETED" -> friendshipFeedService.processDeleted(userId, friendId);
            default -> throw new IllegalArgumentException("Unsupported friendship event type: " + event.eventType());
        }
    }

    private void validateEnvelope(FriendshipUpdatedEvent event) {
        if (
            event == null ||
            event.eventId() == null ||
            event.eventType() == null ||
            event.timestamp() == null ||
            event.data() == null ||
            event.data().userId() == null ||
            event.data().friendId() == null ||
            event.data().userId().equals(event.data().friendId())
        ) {
            throw new IllegalArgumentException("Friendship event envelope is invalid");
        }
        if (event.eventVersion() != SUPPORTED_EVENT_VERSION) {
            throw new IllegalArgumentException("Unsupported friendship event version: " + event.eventVersion());
        }
    }

}
