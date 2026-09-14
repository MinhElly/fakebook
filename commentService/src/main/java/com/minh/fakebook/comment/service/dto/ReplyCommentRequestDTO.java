package com.minh.fakebook.comment.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * DTO for replying to an existing Comment.
 */
public record ReplyCommentRequestDTO(
        @NotNull java.util.UUID parentCommentId,
        @NotBlank String content) implements Serializable {
}