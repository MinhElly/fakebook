package com.minh.fakebook.post.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import io.namastack.outbox.context.OutboxContextProvider;

/**
 * Persists the active trace propagation headers with each outbox record.
 * The outbox worker runs later on another thread, so its in-memory trace context
 * is no longer available when the Kafka record is produced.
 */
@Configuration
public class OutboxTracingConfiguration {

    @Bean
    OutboxContextProvider tracingOutboxContextProvider(Optional<Tracer> tracer, Optional<Propagator> propagator) {
        return () -> {
            if (tracer.isEmpty() || propagator.isEmpty()) {
                return Map.of();
            }

            Span currentSpan = tracer.orElseThrow().currentSpan();
            if (currentSpan == null) {
                return Map.of();
            }
            Map<String, String> propagationHeaders = new HashMap<>();
            propagator.orElseThrow().inject(currentSpan.context(), propagationHeaders, Map::put);
            return Map.copyOf(propagationHeaders);
        };
    }
}
