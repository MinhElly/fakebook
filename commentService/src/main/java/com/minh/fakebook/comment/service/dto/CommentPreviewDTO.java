package com.minh.fakebook.comment.service.dto;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

public record CommentPreviewDTO(UUID id, UUID authorId, String content, Instant createdAt, Instant editedAt, long editCount) implements Serializable {
    public CommentPreviewDTO(UUID id, UUID authorId, String content, Instant createdAt) {
        this(id, authorId, content, createdAt, null, 0);
    }
}
