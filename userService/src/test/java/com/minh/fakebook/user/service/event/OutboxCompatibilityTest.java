package com.minh.fakebook.user.service.event;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class OutboxCompatibilityTest {
    @Test void oldPersistedPayloadTypesStillDeserializeWithTheSameWireShape() throws Exception {
        JsonMapper mapper = JsonMapper.builder().findAndAddModules().build();
        for (String name : java.util.List.of("FriendshipUpdatedEvent", "FriendRequestCreatedEvent", "MediaCleanupEvent")) {
            Class<?> previous = Class.forName("com.minh.fakebook.user.service.dto.events." + name);
            Class<?> current = Class.forName("com.minh.fakebook.user.service.event." + name);
            String json = name.equals("MediaCleanupEvent")
                ? "{\"mediaId\":\"00000000-0000-0000-0000-000000000001\",\"reason\":\"TEST\"}"
                : "{\"eventId\":\"00000000-0000-0000-0000-000000000001\",\"eventType\":\"TEST\",\"eventVersion\":1,\"timestamp\":\"2026-10-05T00:00:00Z\",\"data\":{}}";
            assertThat((tools.jackson.databind.JsonNode) mapper.valueToTree(mapper.readValue(json, previous)))
                .isEqualTo(mapper.valueToTree(mapper.readValue(json, current)));
        }
    }
}
