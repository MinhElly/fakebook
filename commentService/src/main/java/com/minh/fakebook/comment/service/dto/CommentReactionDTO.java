package com.minh.fakebook.comment.service.dto;

import com.minh.fakebook.comment.domain.enumeration.ReactionType;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link com.minh.fakebook.comment.domain.CommentReaction} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class CommentReactionDTO implements Serializable {

    private UUID id;

    @NotNull
    private UUID userId;

    @NotNull
    private ReactionType reactionType;

    @NotNull
    private Instant createdAt;

    private Instant updatedAt;

    @NotNull
    private CommentDTO comment;

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

    public ReactionType getReactionType() {
        return reactionType;
    }

    public void setReactionType(ReactionType reactionType) {
        this.reactionType = reactionType;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public CommentDTO getComment() {
        return comment;
    }

    public void setComment(CommentDTO comment) {
        this.comment = comment;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CommentReactionDTO)) {
            return false;
        }

        CommentReactionDTO commentReactionDTO = (CommentReactionDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, commentReactionDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "CommentReactionDTO{" +
            "id='" + getId() + "'" +
            ", userId='" + getUserId() + "'" +
            ", reactionType='" + getReactionType() + "'" +
            ", createdAt='" + getCreatedAt() + "'" +
            ", updatedAt='" + getUpdatedAt() + "'" +
            ", comment=" + getComment() +
            "}";
    }
}
