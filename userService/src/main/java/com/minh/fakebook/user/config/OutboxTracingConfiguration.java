package com.minh.fakebook.user.config;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import io.namastack.outbox.context.OutboxContextProvider;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
