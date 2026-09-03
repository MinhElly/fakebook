package com.minh.fakebook.post.service.dto;

import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link com.minh.fakebook.post.domain.PostMedia} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class PostMediaDTO implements Serializable {

    private UUID id;

    @NotNull
    private UUID mediaId;

    @NotNull
    @Min(value = 0)
    private Integer displayOrder;

    @NotNull
    private Instant createdAt;

    @NotNull
    private PostDTO post;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getMediaId() {
        return mediaId;
    }

    public void setMediaId(UUID mediaId) {
        this.mediaId = mediaId;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public PostDTO getPost() {
        return post;
    }

    public void setPost(PostDTO post) {
        this.post = post;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PostMediaDTO)) {
            return false;
        }

        PostMediaDTO postMediaDTO = (PostMediaDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, postMediaDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "PostMediaDTO{" +
            "id='" + getId() + "'" +
            ", mediaId='" + getMediaId() + "'" +
            ", displayOrder=" + getDisplayOrder() +
            ", createdAt='" + getCreatedAt() + "'" +
            ", post=" + getPost() +
            "}";
    }
}
