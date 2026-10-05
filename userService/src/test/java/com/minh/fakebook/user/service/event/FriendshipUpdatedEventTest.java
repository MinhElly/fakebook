package com.minh.fakebook.user.service.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class FriendshipUpdatedEventTest {

    @Test
    void createsFriendshipCreatedEvent() {
        UUID userId = UUID.randomUUID();
        UUID friendId = UUID.randomUUID();

        FriendshipUpdatedEvent event = FriendshipUpdatedEvent.created(userId, friendId);

        assertThat(event.eventId()).isNotNull();
        assertThat(event.eventType()).isEqualTo("FRIENDSHIP_CREATED");
        assertThat(event.eventVersion()).isEqualTo(1);
        assertThat(event.timestamp()).isNotNull();
        assertThat(event.data().userId()).isEqualTo(userId);
        assertThat(event.data().friendId()).isEqualTo(friendId);
    }

    @Test
    void createsFriendshipDeletedEvent() {
        UUID userId = UUID.randomUUID();
        UUID friendId = UUID.randomUUID();

        FriendshipUpdatedEvent event = FriendshipUpdatedEvent.deleted(userId, friendId);

        assertThat(event.eventType()).isEqualTo("FRIENDSHIP_DELETED");
        assertThat(event.data().userId()).isEqualTo(userId);
        assertThat(event.data().friendId()).isEqualTo(friendId);
    }
}
