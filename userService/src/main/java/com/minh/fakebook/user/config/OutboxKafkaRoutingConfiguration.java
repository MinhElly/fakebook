package com.minh.fakebook.user.config;

import io.micrometer.tracing.propagation.Propagator;
import io.namastack.outbox.kafka.KafkaOutboxRouting;
import io.namastack.outbox.routing.OutboxRoute;
import io.namastack.outbox.routing.selector.OutboxPayloadSelector;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OutboxKafkaRoutingConfiguration {

    @Bean
    public KafkaOutboxRouting kafkaOutboxRouting(Optional<Propagator> propagator) {
        Set<String> propagationFields = propagator.map(Propagator::fields).map(Set::copyOf).orElseGet(Set::of);

        Consumer<OutboxRoute.Builder> friendRequestRoute = route -> {
            route.target("friend-request-events");
            route.headers((payload, metadata) -> propagationHeaders(metadata.getContext(), propagationFields));
        };
        Consumer<OutboxRoute.Builder> friendshipRoute = route -> {
            route.target("friendship-events");
            route.headers((payload, metadata) -> propagationHeaders(metadata.getContext(), propagationFields));
        };
        Consumer<OutboxRoute.Builder> mediaCleanupRoute = route -> {
            route.target("media-cleanup-topic");
            route.headers((payload, metadata) -> propagationHeaders(metadata.getContext(), propagationFields));
        };

        return KafkaOutboxRouting.builder()
            .route(OutboxPayloadSelector.contextValue("destination", "friend-request-events"), friendRequestRoute)
            .route(OutboxPayloadSelector.contextValue("destination", "media-cleanup-topic"), mediaCleanupRoute)
            .defaults(friendshipRoute)
            .build();
    }

    private Map<String, String> propagationHeaders(Map<String, String> context, Set<String> propagationFields) {
        return context.entrySet().stream()
            .filter(entry -> propagationFields.contains(entry.getKey()))
            .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
