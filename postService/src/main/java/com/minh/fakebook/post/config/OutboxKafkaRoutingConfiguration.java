package com.minh.fakebook.post.config;

import io.namastack.outbox.kafka.KafkaOutboxRouting;
import io.namastack.outbox.routing.OutboxRoute;
import io.namastack.outbox.routing.selector.OutboxPayloadSelector;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Configuration
public class OutboxKafkaRoutingConfiguration {
    @Bean
    public KafkaOutboxRouting kafkaOutboxRouting() {

        Consumer<OutboxRoute.Builder> reactionRoute =
            route -> route.target("post-reaction-events");

        Consumer<OutboxRoute.Builder> defaultRoute =
            route -> route.target("post-events");

        return KafkaOutboxRouting.builder()
            .route(
                OutboxPayloadSelector.contextValue(
                    "destination",
                    "post-reaction-events"
                ),
                reactionRoute
            )
            .defaults(defaultRoute)
            .build();
    }
}
