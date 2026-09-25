package com.minh.fakebook.gateway.realtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

@Configuration
public class ReactionEventConsumerConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(ReactionEventConsumerConfiguration.class);

    @Bean
    public Consumer<GatewayEventEnvelope> reactionEventConsumer(RealtimeEventHub eventHub) {
        return envelope -> {
            if (envelope == null || !"POST_REACTION_CHANGED".equals(envelope.eventType())) {
                return;
            }
            Map<String, Object> data = envelope.data();
            if (data == null) {
                LOG.warn("Reaction event {} has no data", envelope.eventId());
                return;
            }
            Object rawPostId = data.get("postId");
            if (rawPostId == null) {
                LOG.warn("Reaction event {} has no postId", envelope.eventId());
                return;
            }
            try {
                UUID postId = UUID.fromString(rawPostId.toString());
                eventHub.publish(
                    new RealtimeEvent(
                        envelope.eventId(),
                        envelope.eventType(),
                        postId,
                        null,
                        null,
                        null,
                        envelope.timestamp() != null ? envelope.timestamp() : Instant.now()
                    )
                );
            } catch (IllegalArgumentException e) {
                LOG.warn("Invalid postId in reaction event {}", envelope.eventId());
            }
        };
    }
}
