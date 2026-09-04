package com.minh.fakebook.user.service.dto;

import jakarta.persistence.Lob;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A DTO for the {@link com.minh.fakebook.user.domain.UserProfile} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class UserProfileDTO implements Serializable {
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private UUID id;
    
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    @NotNull
    @Size(max = 50)
    private String username;

    @NotNull
    @Size(max = 100)
    private String displayName;

    @Lob
    private String bio;

    private UUID avatarMediaId;

    private UUID coverMediaId;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    @NotNull
    private Instant createdAt;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private Instant updatedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public UUID getAvatarMediaId() {
        return avatarMediaId;
    }

    public void setAvatarMediaId(UUID avatarMediaId) {
        this.avatarMediaId = avatarMediaId;
    }

    public UUID getCoverMediaId() {
        return coverMediaId;
    }

    public void setCoverMediaId(UUID coverMediaId) {
        this.coverMediaId = coverMediaId;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UserProfileDTO)) {
            return false;
        }

        UserProfileDTO userProfileDTO = (UserProfileDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, userProfileDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "UserProfileDTO{" +
            "id='" + getId() + "'" +
            ", username='" + getUsername() + "'" +
            ", displayName='" + getDisplayName() + "'" +
            ", bio='" + getBio() + "'" +
            ", avatarMediaId='" + getAvatarMediaId() + "'" +
            ", coverMediaId='" + getCoverMediaId() + "'" +
            ", createdAt='" + getCreatedAt() + "'" +
            ", updatedAt='" + getUpdatedAt() + "'" +
            "}";
    }
}
