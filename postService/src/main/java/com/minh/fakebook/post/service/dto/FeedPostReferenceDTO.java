package com.minh.fakebook.post.service.dto;

import com.minh.fakebook.post.domain.enumeration.PostVisibility;
import java.time.Instant;
import java.util.UUID;

/** Minimal post metadata needed to build feed projections. */
public record FeedPostReferenceDTO(UUID postId, UUID authorId, PostVisibility visibility, Instant createdAt) {}
