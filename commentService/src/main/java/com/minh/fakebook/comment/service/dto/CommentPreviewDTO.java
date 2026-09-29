package com.minh.fakebook.comment.service.dto;

import java.time.Instant;
import java.util.UUID;

public record CommentPreviewDTO(UUID id, UUID authorId, String content, Instant createdAt) {}
