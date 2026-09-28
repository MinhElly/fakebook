package com.minh.fakebook.post.service.dto;

import com.minh.fakebook.post.domain.enumeration.ReactionType;
import jakarta.validation.constraints.NotNull;

public record SetPostReactionRequest(@NotNull ReactionType reactionType) {}
