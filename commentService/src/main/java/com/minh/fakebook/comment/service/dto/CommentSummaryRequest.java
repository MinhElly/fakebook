package com.minh.fakebook.comment.service.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record CommentSummaryRequest(@NotEmpty @Size(max = 50) List<UUID> postIds) {}
