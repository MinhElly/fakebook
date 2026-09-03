package com.minh.fakebook.feed.service.dto;

import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link com.minh.fakebook.feed.domain.FeedItem} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class FeedItemDTO implements Serializable {

    private UUID id;

    @NotNull
    private UUID userId;

    @NotNull
    private UUID postId;

    @NotNull
    private Instant createdAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getPostId() {
        return postId;
    }

    public void setPostId(UUID postId) {
        this.postId = postId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FeedItemDTO)) {
            return false;
        }

        FeedItemDTO feedItemDTO = (FeedItemDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, feedItemDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "FeedItemDTO{" +
            "id='" + getId() + "'" +
            ", userId='" + getUserId() + "'" +
            ", postId='" + getPostId() + "'" +
            ", createdAt='" + getCreatedAt() + "'" +
            "}";
    }
}
