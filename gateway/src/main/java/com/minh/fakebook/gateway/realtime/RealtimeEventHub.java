package com.minh.fakebook.gateway.realtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Component
public class RealtimeEventHub {
    private static final Logger LOG = LoggerFactory.getLogger(RealtimeEventHub.class);
    private final Sinks.Many<RealtimeEvent> sink = Sinks.many().multicast().directBestEffort();

    public void publish(RealtimeEvent event) {
        Sinks.EmitResult result = sink.tryEmitNext(event);
        if (result.isFailure() && result != Sinks.EmitResult.FAIL_ZERO_SUBSCRIBER) {
            LOG.warn("Could not publish realtime event {}: {}", event.eventId(), result);
        }
    }

    public Flux<RealtimeEvent> events() {
        return sink.asFlux();
    }
}
