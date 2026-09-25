package com.minh.fakebook.gateway.web.rest;

import com.minh.fakebook.gateway.realtime.RealtimeEvent;
import com.minh.fakebook.gateway.realtime.RealtimeEventHub;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Locale;
import java.util.UUID;

@RestController
@RequestMapping("/api/realtime")
public class RealtimeResource {
    private final RealtimeEventHub eventHub;

    public RealtimeResource(RealtimeEventHub eventHub) {
        this.eventHub = eventHub;
    }

    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<RealtimeEvent>> events(@AuthenticationPrincipal Jwt jwt) {
        UUID currentUserId = UUID.fromString(jwt.getSubject());
        Flux<ServerSentEvent<RealtimeEvent>> realtimeEvents = eventHub
            .events()
            .filter(event -> event.recipientId() == null || currentUserId.equals(event.recipientId()))
            .map(event ->
                ServerSentEvent.builder(event)
                    .id(event.eventId().toString())
                    .event(toSseEventName(event.eventType()))
                    .build()
            );
        Flux<ServerSentEvent<RealtimeEvent>> heartbeat = Flux.interval(Duration.ofSeconds(20)).map(sequence ->
            ServerSentEvent.<RealtimeEvent>builder().comment("heartbeat").build()
        );
        return Flux.merge(realtimeEvents, heartbeat);
    }

    private String toSseEventName(String eventType) {
        return eventType.toLowerCase(Locale.ROOT).replace('_', '-');
    }
}
