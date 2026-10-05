package com.minh.fakebook.comment.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.UUID;

/**
 * DTO for replying to an existing Comment.
 */
public record ReplyCommentRequestDTO(
        @NotNull UUID parentCommentId,
        @NotBlank String content) implements Serializable {
}