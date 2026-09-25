package com.minh.fakebook.gateway.web.rest;

import com.minh.fakebook.gateway.realtime.RealtimeEvent;
import com.minh.fakebook.gateway.realtime.RealtimeEventHub;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.Duration;

@RestController
@RequestMapping("/api/realtime")
public class RealtimeResource {
    private final RealtimeEventHub eventHub;

    public RealtimeResource(RealtimeEventHub eventHub) {
        this.eventHub = eventHub;
    }

    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<RealtimeEvent>> events() {
        Flux<ServerSentEvent<RealtimeEvent>> reactionEvents = eventHub
            .events()
            .map(event ->
                ServerSentEvent.builder(event).id(event.eventId().toString()).event("post-reaction-changed").build()
            );
        Flux<ServerSentEvent<RealtimeEvent>> heartbeat = Flux.interval(Duration.ofSeconds(20)).map(sequence ->
            ServerSentEvent.<RealtimeEvent>builder().comment("heartbeat").build()
        );
        return Flux.merge(reactionEvents, heartbeat);
    }
}
