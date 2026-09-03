package com.minh.fakebook.user.service.dto;

import com.minh.fakebook.user.domain.enumeration.FriendRequestStatus;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link com.minh.fakebook.user.domain.FriendRequest} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class FriendRequestDTO implements Serializable {

    private UUID id;

    @NotNull
    private FriendRequestStatus status;

    @NotNull
    private Instant createdAt;

    private Instant respondedAt;

    @NotNull
    private UserProfileDTO sender;

    @NotNull
    private UserProfileDTO receiver;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public FriendRequestStatus getStatus() {
        return status;
    }

    public void setStatus(FriendRequestStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getRespondedAt() {
        return respondedAt;
    }

    public void setRespondedAt(Instant respondedAt) {
        this.respondedAt = respondedAt;
    }

    public UserProfileDTO getSender() {
        return sender;
    }

    public void setSender(UserProfileDTO sender) {
        this.sender = sender;
    }

    public UserProfileDTO getReceiver() {
        return receiver;
    }

    public void setReceiver(UserProfileDTO receiver) {
        this.receiver = receiver;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FriendRequestDTO)) {
            return false;
        }

        FriendRequestDTO friendRequestDTO = (FriendRequestDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, friendRequestDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "FriendRequestDTO{" +
            "id='" + getId() + "'" +
            ", status='" + getStatus() + "'" +
            ", createdAt='" + getCreatedAt() + "'" +
            ", respondedAt='" + getRespondedAt() + "'" +
            ", sender=" + getSender() +
            ", receiver=" + getReceiver() +
            "}";
    }
}
