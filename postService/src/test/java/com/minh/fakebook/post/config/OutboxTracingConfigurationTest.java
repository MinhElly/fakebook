package com.minh.fakebook.post.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import io.namastack.outbox.context.OutboxContextProvider;

class OutboxTracingConfigurationTest {

    private final Tracer tracer = mock(Tracer.class);
    private final Propagator propagator = mock(Propagator.class);
    private final OutboxTracingConfiguration configuration = new OutboxTracingConfiguration();

    @Test
    void shouldReturnEmptyContextWhenThereIsNoActiveSpan() {
        OutboxContextProvider provider = configuration.tracingOutboxContextProvider(Optional.of(tracer), Optional.of(propagator));

        assertThat(provider.provide()).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldPersistHeadersProducedByTheConfiguredPropagator() {
        Span span = mock(Span.class);
        TraceContext traceContext = mock(TraceContext.class);
        when(tracer.currentSpan()).thenReturn(span);
        when(span.context()).thenReturn(traceContext);
        doAnswer(invocation -> {
            Map<String, String> carrier = invocation.getArgument(1);
            Propagator.Setter<Map<String, String>> setter = invocation.getArgument(2);
            setter.set(carrier, "traceparent", "00-trace-span-01");
            return null;
        }).when(propagator).inject(any(), any(), any());

        OutboxContextProvider provider = configuration.tracingOutboxContextProvider(Optional.of(tracer), Optional.of(propagator));

        assertThat(provider.provide()).containsEntry("traceparent", "00-trace-span-01");
    }

    @Test
    void shouldReturnEmptyContextWhenTracingBeansAreDisabled() {
        OutboxContextProvider provider = configuration.tracingOutboxContextProvider(Optional.empty(), Optional.empty());

        assertThat(provider.provide()).isEmpty();
    }
}
