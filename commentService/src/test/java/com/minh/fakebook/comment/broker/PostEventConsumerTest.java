package com.minh.fakebook.comment.broker;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import tools.jackson.databind.ObjectMapper;

class PostEventConsumerTest {

    private ObjectMapper objectMapper;
    private JdbcTemplate jdbcTemplate;
    private PostEventConsumer postEventConsumer;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        jdbcTemplate = mock(JdbcTemplate.class);
        postEventConsumer = new PostEventConsumer(objectMapper, jdbcTemplate);
    }

    @Test
    void shouldProcessPostCreatedEvent() {
        UUID postId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        String json = "{\"eventType\":\"POST_CREATED\",\"data\":{\"id\":\"%s\",\"authorId\":\"%s\",\"status\":\"ACTIVE\",\"visibility\":\"PUBLIC\"}}".formatted(postId, authorId);
        Acknowledgment ack = mock(Acknowledgment.class);
        Message<String> message = MessageBuilder.withPayload(json)
            .setHeader(KafkaHeaders.ACKNOWLEDGMENT, ack)
            .build();

        postEventConsumer.accept(message);

        verify(jdbcTemplate).update(
            anyString(),
            eq(postId.toString()),
            eq(authorId.toString()),
            eq("ACTIVE"),
            eq("PUBLIC"),
            eq(authorId.toString()),
            eq("ACTIVE"),
            eq("PUBLIC")
        );
        verify(ack).acknowledge();
    }

    @Test
    void shouldProcessPostDeletedEvent() {
        UUID postId = UUID.randomUUID();
        String json = "{\"eventType\":\"POST_DELETED\",\"data\":{\"id\":\"%s\"}}".formatted(postId);
        Acknowledgment ack = mock(Acknowledgment.class);
        Message<String> message = MessageBuilder.withPayload(json)
            .setHeader(KafkaHeaders.ACKNOWLEDGMENT, ack)
            .build();

        postEventConsumer.accept(message);

        verify(jdbcTemplate).update(eq("DELETE FROM post_cache WHERE id = ?"), eq(postId.toString()));
        verify(ack).acknowledge();
    }

    @Test
    void shouldRejectWhenEventTypeIsMissing() {
        UUID postId = UUID.randomUUID();
        String json = "{\"data\":{\"id\":\"%s\"}}".formatted(postId);
        Message<String> message = MessageBuilder.withPayload(json).build();

        assertThatThrownBy(() -> postEventConsumer.accept(message))
            .isInstanceOf(RuntimeException.class)
            .hasCauseInstanceOf(IllegalArgumentException.class)
            .getCause()
            .hasMessageContaining("missing eventType");
    }

    @Test
    void shouldRejectWhenAuthorIdIsMissingOnPostCreated() {
        UUID postId = UUID.randomUUID();
        String json = "{\"eventType\":\"POST_CREATED\",\"data\":{\"id\":\"%s\",\"visibility\":\"PUBLIC\"}}".formatted(postId);
        Message<String> message = MessageBuilder.withPayload(json).build();

        assertThatThrownBy(() -> postEventConsumer.accept(message))
            .isInstanceOf(RuntimeException.class)
            .hasCauseInstanceOf(IllegalArgumentException.class)
            .getCause()
            .hasMessageContaining("missing authorId");
    }

    @Test
    void shouldRejectWhenVisibilityIsMissingOnPostCreated() {
        UUID postId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        String json = "{\"eventType\":\"POST_CREATED\",\"data\":{\"id\":\"%s\",\"authorId\":\"%s\"}}".formatted(postId, authorId);
        Message<String> message = MessageBuilder.withPayload(json).build();

        assertThatThrownBy(() -> postEventConsumer.accept(message))
            .isInstanceOf(RuntimeException.class)
            .hasCauseInstanceOf(IllegalArgumentException.class)
            .getCause()
            .hasMessageContaining("visibility must not be null");
    }

    @Test
    void shouldRejectWhenVisibilityIsInvalid() {
        UUID postId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        String json = "{\"eventType\":\"POST_CREATED\",\"data\":{\"id\":\"%s\",\"authorId\":\"%s\",\"visibility\":\"INVALID_MODE\"}}".formatted(postId, authorId);
        Message<String> message = MessageBuilder.withPayload(json).build();

        assertThatThrownBy(() -> postEventConsumer.accept(message))
            .isInstanceOf(RuntimeException.class)
            .hasCauseInstanceOf(IllegalArgumentException.class)
            .getCause()
            .hasMessageContaining("Invalid post event visibility");
    }
}
