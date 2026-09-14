package com.minh.fakebook.comment.service.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO for updating an existing Comment
 */
public record UpdateCommentRequestDTO(
        @NotBlank String content) {
}