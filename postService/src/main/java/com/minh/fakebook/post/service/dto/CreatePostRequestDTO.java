package com.minh.fakebook.post.service.dto;

import com.minh.fakebook.post.domain.enumeration.PostVisibility;
import java.util.List;
import java.util.UUID;

/**
 * Data Transfer Object representing the client request to create a new post.
 * Extracted to a separate file to comply with the rule forbidding Inner Classes.
 */
public record CreatePostRequestDTO(
    String content,
    PostVisibility visibility,
    List<UUID> mediaIds
) {}
