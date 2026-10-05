package com.minh.fakebook.post.service.dto;

import com.minh.fakebook.post.domain.enumeration.ReactionType;
import java.time.Instant;
import java.util.UUID;

public record PostReactorDTO(UUID userId, ReactionType reactionType, Instant reactedAt) {}
