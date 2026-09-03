package com.minh.fakebook.user.domain;

import com.minh.fakebook.user.domain.enumeration.FriendRequestStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * A FriendRequest.
 */
@Entity
@Table(name = "friend_requests")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class FriendRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "id", length = 36)
    private UUID id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private FriendRequestStatus status;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "responded_at")
    private Instant respondedAt;

    @ManyToOne(optional = false)
    @NotNull
    private UserProfile sender;

    @ManyToOne(optional = false)
    @NotNull
    private UserProfile receiver;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public FriendRequest id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public FriendRequestStatus getStatus() {
        return this.status;
    }

    public FriendRequest status(FriendRequestStatus status) {
        this.setStatus(status);
        return this;
    }

    public void setStatus(FriendRequestStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }

    public FriendRequest createdAt(Instant createdAt) {
        this.setCreatedAt(createdAt);
        return this;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getRespondedAt() {
        return this.respondedAt;
    }

    public FriendRequest respondedAt(Instant respondedAt) {
        this.setRespondedAt(respondedAt);
        return this;
    }

    public void setRespondedAt(Instant respondedAt) {
        this.respondedAt = respondedAt;
    }

    public UserProfile getSender() {
        return this.sender;
    }

    public void setSender(UserProfile userProfile) {
        this.sender = userProfile;
    }

    public FriendRequest sender(UserProfile userProfile) {
        this.setSender(userProfile);
        return this;
    }

    public UserProfile getReceiver() {
        return this.receiver;
    }

    public void setReceiver(UserProfile userProfile) {
        this.receiver = userProfile;
    }

    public FriendRequest receiver(UserProfile userProfile) {
        this.setReceiver(userProfile);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FriendRequest)) {
            return false;
        }
        return getId() != null && getId().equals(((FriendRequest) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "FriendRequest{" +
            "id=" + getId() +
            ", status='" + getStatus() + "'" +
            ", createdAt='" + getCreatedAt() + "'" +
            ", respondedAt='" + getRespondedAt() + "'" +
            "}";
    }
}
