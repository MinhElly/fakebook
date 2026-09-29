package com.minh.fakebook.comment.service.dto;

import java.util.UUID;

public record CommentSummaryDTO(UUID postId, long commentCount, CommentPreviewDTO previewComment) {}
