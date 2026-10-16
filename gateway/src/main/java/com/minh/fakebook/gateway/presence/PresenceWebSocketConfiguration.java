package com.minh.fakebook.gateway.presence;

import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.server.support.HandshakeWebSocketService;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;
import org.springframework.web.reactive.socket.server.upgrade.ReactorNettyRequestUpgradeStrategy;

@Configuration
public class PresenceWebSocketConfiguration {
    @Bean
    public SimpleUrlHandlerMapping presenceWebSocketMapping(PresenceWebSocketHandler handler) {
        return new SimpleUrlHandlerMapping(Map.of("/api/presence/ws", handler), -2);
    }

    @Bean
    public WebSocketHandlerAdapter presenceWebSocketAdapter() {
        var strategy = new ReactorNettyRequestUpgradeStrategy(() ->
            reactor.netty.http.server.WebsocketServerSpec.builder().maxFramePayloadLength(8192));
        var adapter = new WebSocketHandlerAdapter(new HandshakeWebSocketService(strategy));
        adapter.setOrder(-1);
        return adapter;
    }
}
