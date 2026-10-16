package com.minh.fakebook.user.service.dto;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Cache DTO for user profile summaries.
 * Immutable representation optimized for feed hydration and profile lookups.
 */
public record UserProfileCacheDTO(
    UUID id,
    String username,
    String displayName,
    String bio,
    String location,
    String education,
    String work,
    UUID avatarMediaId,
    UUID coverMediaId,
    Instant createdAt,
    Instant updatedAt
) implements Serializable {

    public static final UserProfileCacheDTO EMPTY = new UserProfileCacheDTO(
        new UUID(0L, 0L),
        "",
        "",
        null,
        null,
        null,
        null,
        null,
        null,
        Instant.EPOCH,
        Instant.EPOCH
    );

    public boolean isEmpty() {
        return id != null && id.getMostSignificantBits() == 0L && id.getLeastSignificantBits() == 0L;
    }

    public static UserProfileCacheDTO fromDTO(UserProfileDTO dto) {
        if (dto == null) {
            return null;
        }
        return new UserProfileCacheDTO(
            dto.getId(),
            dto.getUsername(),
            dto.getDisplayName(),
            dto.getBio(),
            dto.getLocation(),
            dto.getEducation(),
            dto.getWork(),
            dto.getAvatarMediaId(),
            dto.getCoverMediaId(),
            dto.getCreatedAt(),
            dto.getUpdatedAt()
        );
    }

    public UserProfileDTO toDTO() {
        UserProfileDTO dto = new UserProfileDTO();
        dto.setId(this.id);
        dto.setUsername(this.username);
        dto.setDisplayName(this.displayName);
        dto.setBio(this.bio);
        dto.setLocation(this.location);
        dto.setEducation(this.education);
        dto.setWork(this.work);
        dto.setAvatarMediaId(this.avatarMediaId);
        dto.setCoverMediaId(this.coverMediaId);
        dto.setCreatedAt(this.createdAt);
        dto.setUpdatedAt(this.updatedAt);
        return dto;
    }
}
