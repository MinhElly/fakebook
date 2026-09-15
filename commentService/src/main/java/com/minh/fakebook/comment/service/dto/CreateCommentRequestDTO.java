package com.minh.fakebook.comment.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;


/**
 * DTO for creating a new Comment.
 */
public record CreateCommentRequestDTO(
        @NotNull UUID postId,
        @NotBlank String content) {
}
