package com.minh.fakebook.gateway.broker;

import com.minh.fakebook.gateway.presence.PresenceBus;
import java.time.Duration;
import java.util.UUID;
import java.util.function.Consumer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PresenceFriendshipConsumerConfiguration {
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    public record FriendshipEvent(String eventType, Data data) {
        public record Data(UUID userId, UUID friendId) {}
    }

    @Bean
    public Consumer<FriendshipEvent> presenceFriendshipConsumer(PresenceBus bus) {
        return event -> {
            if (event == null || event.data() == null || event.data().userId() == null || event.data().friendId() == null ||
                !("FRIENDSHIP_CREATED".equals(event.eventType()) || "FRIENDSHIP_DELETED".equals(event.eventType()))) {
                throw new IllegalArgumentException("Invalid friendship presence event");
            }
            // Kafka listener thread: propagate errors so the binder retries. Every Gateway
            // receives the Redis hint; periodic DB reconciliation covers a lost hint.
            bus.invalidateFriendships().block(Duration.ofSeconds(5));
        };
    }
}
