package com.minh.fakebook.post.service.dto;

import com.minh.fakebook.post.domain.enumeration.PostStatus;
import com.minh.fakebook.post.domain.enumeration.PostVisibility;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Cache DTO for post content, media attachments, and visibility metadata.
 * Independent of viewer authorization; authorization is performed dynamically on access.
 * Supports negative caching via EMPTY sentinel.
 */
public record PostContentCacheDTO(
    UUID id,
    UUID authorId,
    String content,
    PostVisibility visibility,
    PostStatus status,
    Instant createdAt,
    Instant updatedAt,
    List<UUID> mediaIds,
    Set<UUID> taggedUserIds,
    Long version
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public static final PostContentCacheDTO EMPTY = new PostContentCacheDTO(
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        List.of(),
        Set.of(),
        null
    );

    public PostContentCacheDTO {
        mediaIds = mediaIds != null ? List.copyOf(mediaIds) : List.of();
        taggedUserIds = taggedUserIds != null ? Set.copyOf(taggedUserIds) : Set.of();
    }

    public boolean isEmpty() {
        return id == null;
    }
}
