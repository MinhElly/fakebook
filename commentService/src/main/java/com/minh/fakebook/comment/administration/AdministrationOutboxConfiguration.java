package com.minh.fakebook.comment.administration;

import org.springframework.context.annotation.*;
import io.namastack.outbox.kafka.KafkaOutboxRouting;
import io.namastack.outbox.routing.selector.OutboxPayloadSelector;
@Configuration
public class AdministrationOutboxConfiguration {
    @Bean public KafkaOutboxRouting kafkaOutboxRouting() {
        return KafkaOutboxRouting.builder()
            .route(OutboxPayloadSelector.contextValue("destination","admin-audit-events"),(java.util.function.Consumer<io.namastack.outbox.routing.OutboxRoute.Builder>) r -> r.target("admin-audit-events"))
            .route(OutboxPayloadSelector.contextValue("destination","content-moderation-events"),(java.util.function.Consumer<io.namastack.outbox.routing.OutboxRoute.Builder>) r -> r.target("content-moderation-events"))
            .defaults((java.util.function.Consumer<io.namastack.outbox.routing.OutboxRoute.Builder>) r -> r.target("comment-events")).build();
    }
}
