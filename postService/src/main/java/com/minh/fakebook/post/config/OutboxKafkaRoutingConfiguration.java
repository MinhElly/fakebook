package com.minh.fakebook.post.config;

import io.namastack.outbox.kafka.KafkaOutboxRouting;
import io.namastack.outbox.routing.OutboxRoute;
import io.namastack.outbox.routing.selector.OutboxPayloadSelector;
import io.micrometer.tracing.propagation.Propagator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Configuration
public class OutboxKafkaRoutingConfiguration {
    @Bean
    public KafkaOutboxRouting kafkaOutboxRouting(Optional<Propagator> propagator) {
        Set<String> propagationFields = propagator
            .map(Propagator::fields)
            .map(Set::copyOf)
            .orElseGet(Set::of);

        Consumer<OutboxRoute.Builder> reactionRoute = route -> {
            route.target("post-reaction-events");
            route.headers((payload, metadata) -> propagationHeaders(metadata.getContext(), propagationFields));
        };

        Consumer<OutboxRoute.Builder> mediaCleanupRoute = route -> {
            route.target("media-cleanup-topic");
            route.headers((payload, metadata) -> propagationHeaders(metadata.getContext(), propagationFields));
        };

        Consumer<OutboxRoute.Builder> defaultRoute = route -> {
            route.target("post-events");
            route.headers((payload, metadata) -> propagationHeaders(metadata.getContext(), propagationFields));
        };

        return KafkaOutboxRouting.builder()
            .route(
                OutboxPayloadSelector.contextValue("destination", "media-cleanup-topic"),
                mediaCleanupRoute
            )
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

    private Map<String, String> propagationHeaders(Map<String, String> context, Set<String> propagationFields) {
        return context.entrySet().stream()
            .filter(entry -> propagationFields.contains(entry.getKey()))
            .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
