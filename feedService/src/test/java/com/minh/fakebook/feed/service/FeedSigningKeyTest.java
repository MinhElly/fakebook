package com.minh.fakebook.feed.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import com.minh.fakebook.feed.client.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.ObjectMapper;

class FeedSigningKeyTest {
    @ParameterizedTest
    @ValueSource(strings = {"", "short", "REPLACE_ME_WITH_A_RANDOM_SECRET_AT_LEAST_32_CHARACTERS"})
    void invalidKeyFailsBeforeServingRequests(String key) {
        assertThatThrownBy(() -> new HomeFeedService(mock(JdbcTemplate.class), new ObjectMapper(), mock(FeedReadClient.class), mock(SocialContextClient.class), key))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("cursor-secret");
    }
    @org.junit.jupiter.api.Test
    void legacyPagePayloadRemainsReadableAndProjectionCacheIsNotPersisted() {
        ObjectMapper mapper = new ObjectMapper();
        var page = mapper.readValue("{\"items\":[],\"nextCursor\":null,\"hasMore\":false,\"loginRequired\":true}", HomeFeedService.Page.class);
        org.assertj.core.api.Assertions.assertThat(page.items()).isEmpty();
        var state = new HomeFeedService.State();
        state.projection = java.util.List.of();
        org.assertj.core.api.Assertions.assertThat(mapper.writeValueAsString(state)).doesNotContain("projection");
    }
}
