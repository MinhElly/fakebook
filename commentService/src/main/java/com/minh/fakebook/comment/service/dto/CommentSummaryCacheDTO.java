package com.minh.fakebook.comment.service.dto;

import java.io.Serializable;

public record CommentSummaryCacheDTO(
    long commentCount,
    CommentPreviewDTO previewComment
) implements Serializable {}
