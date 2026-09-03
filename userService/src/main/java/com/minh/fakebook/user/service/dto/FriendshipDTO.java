package com.minh.fakebook.user.service.dto;

import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link com.minh.fakebook.user.domain.Friendship} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class FriendshipDTO implements Serializable {

    private UUID id;

    @NotNull
    private Instant createdAt;

    @NotNull
    private UserProfileDTO user;

    @NotNull
    private UserProfileDTO friend;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public UserProfileDTO getUser() {
        return user;
    }

    public void setUser(UserProfileDTO user) {
        this.user = user;
    }

    public UserProfileDTO getFriend() {
        return friend;
    }

    public void setFriend(UserProfileDTO friend) {
        this.friend = friend;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FriendshipDTO)) {
            return false;
        }

        FriendshipDTO friendshipDTO = (FriendshipDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, friendshipDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "FriendshipDTO{" +
            "id='" + getId() + "'" +
            ", createdAt='" + getCreatedAt() + "'" +
            ", user=" + getUser() +
            ", friend=" + getFriend() +
            "}";
    }
}
