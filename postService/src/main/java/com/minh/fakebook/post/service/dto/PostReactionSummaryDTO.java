package com.minh.fakebook.post.service.dto;

import com.minh.fakebook.post.domain.enumeration.ReactionType;

import java.util.Map;
import java.util.UUID;

public record PostReactionSummaryDTO(UUID postId, long totalCount, Map<ReactionType, Long> counts, ReactionType myReaction) {}
