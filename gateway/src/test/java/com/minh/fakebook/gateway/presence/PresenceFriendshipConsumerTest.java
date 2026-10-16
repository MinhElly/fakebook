package com.minh.fakebook.gateway.presence;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.minh.fakebook.gateway.broker.PresenceFriendshipConsumerConfiguration;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import tools.jackson.databind.json.JsonMapper;

class PresenceFriendshipConsumerTest {
    @Test
    void acceptsTheFullUserServiceEnvelopeAndInvalidatesEveryGateway() {
        var event = JsonMapper.builder().build().readValue("""
            {"eventId":"11111111-1111-4111-8111-111111111111", "eventType":"FRIENDSHIP_DELETED",
             "eventVersion":1, "timestamp":"2026-10-16T00:00:00Z",
             "data":{"userId":"22222222-2222-4222-8222-222222222222",
                     "friendId":"33333333-3333-4333-8333-333333333333"}}
            """, PresenceFriendshipConsumerConfiguration.FriendshipEvent.class);
        var bus = mock(PresenceBus.class);
        when(bus.invalidateFriendships()).thenReturn(Mono.empty());
        new PresenceFriendshipConsumerConfiguration().presenceFriendshipConsumer(bus).accept(event);
        verify(bus).invalidateFriendships();
    }

    @Test
    void propagatesRedisFailureForKafkaRetryAndRejectsInvalidEnvelopes() {
        var bus = mock(PresenceBus.class);
        when(bus.invalidateFriendships()).thenReturn(Mono.error(new IllegalStateException("Redis unavailable")));
        var consumer = new PresenceFriendshipConsumerConfiguration().presenceFriendshipConsumer(bus);
        assertThatThrownBy(() -> consumer.accept(null)).isInstanceOf(IllegalArgumentException.class);
        var data = new PresenceFriendshipConsumerConfiguration.FriendshipEvent.Data(java.util.UUID.randomUUID(), java.util.UUID.randomUUID());
        assertThatThrownBy(() -> consumer.accept(new PresenceFriendshipConsumerConfiguration.FriendshipEvent("UNKNOWN", data)))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> consumer.accept(new PresenceFriendshipConsumerConfiguration.FriendshipEvent("FRIENDSHIP_CREATED", data)))
            .hasMessageContaining("Redis unavailable");
    }
}
