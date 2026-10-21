package com.minh.fakebook.gateway.config;

import static org.assertj.core.api.Assertions.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

class PlaybackTicketMatcherTest {
    @Test void delegatesOnlyScopedReadRequestsToMediaForTicketAuthentication() {
        String path = "/services/mediaservice/api/media/" + UUID.randomUUID() + "/file";
        String ticket = UUID.randomUUID().toString() + UUID.randomUUID();
        var matcher = SecurityConfiguration.playbackTicketMatcher();
        assertThat(matcher.matches(MockServerWebExchange.from(MockServerHttpRequest.get(path + "?ticket=" + ticket))).block().isMatch()).isTrue();
        assertThat(matcher.matches(MockServerWebExchange.from(MockServerHttpRequest.get(path))).block().isMatch()).isFalse();
        assertThat(matcher.matches(MockServerWebExchange.from(MockServerHttpRequest.post(path + "?ticket=" + ticket))).block().isMatch()).isFalse();
        assertThat(matcher.matches(MockServerWebExchange.from(MockServerHttpRequest.get("/services/postservice/api/posts?ticket=" + ticket))).block().isMatch()).isFalse();
    }
}
