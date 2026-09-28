package com.minh.fakebook.feed.client.dto;

import java.time.Instant;
import java.util.UUID;

public record FeedPostReferenceDTO(UUID postId, UUID authorId, String visibility, Instant createdAt) {}
