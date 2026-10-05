package com.minh.fakebook.gateway.realtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;
import java.util.function.Consumer;

@Configuration
public class FriendRequestEventConsumerConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(FriendRequestEventConsumerConfiguration.class);

    @Bean
    public Consumer<FriendRequestGatewayEvent> friendRequestEventConsumer(RealtimeEventHub eventHub) {
        return event -> {
            if (event == null || !"FRIEND_REQUEST_CREATED".equals(event.eventType()) || event.data() == null) {
                return;
            }
            if (
                event.eventId() == null ||
                event.data().requestId() == null ||
                event.data().senderId() == null ||
                event.data().receiverId() == null
            ) {
                LOG.warn("Incomplete friend request realtime event {}", event.eventId());
                return;
            }

            eventHub.publish(
                new RealtimeEvent(
                    event.eventId(),
                    event.eventType(),
                    null,
                    event.data().receiverId(),
                    event.data().requestId(),
                    event.data().senderId(),
                    event.timestamp() != null ? event.timestamp() : Instant.now()
                )
            );
        };
    }
}
