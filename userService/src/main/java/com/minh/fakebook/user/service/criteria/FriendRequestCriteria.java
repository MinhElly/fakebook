package com.minh.fakebook.user.service.criteria;

import com.minh.fakebook.user.domain.enumeration.FriendRequestStatus;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link com.minh.fakebook.user.domain.FriendRequest} entity. This class is used
 * in {@link com.minh.fakebook.user.web.rest.FriendRequestResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /friend-requests?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class FriendRequestCriteria implements Serializable, Criteria {

    /**
     * Class for filtering FriendRequestStatus
     */
    public static class FriendRequestStatusFilter extends Filter<FriendRequestStatus> {

        public FriendRequestStatusFilter() {}

        public FriendRequestStatusFilter(FriendRequestStatusFilter filter) {
            super(filter);
        }

        @Override
        public FriendRequestStatusFilter copy() {
            return new FriendRequestStatusFilter(this);
        }
    }

    @Serial
    private static final long serialVersionUID = 1L;

    private UUIDFilter id;

    private FriendRequestStatusFilter status;

    private InstantFilter createdAt;

    private InstantFilter respondedAt;

    private UUIDFilter senderId;

    private UUIDFilter receiverId;

    private Boolean distinct;

    public FriendRequestCriteria() {}

    public FriendRequestCriteria(FriendRequestCriteria other) {
        this.id = other.optionalId().map(UUIDFilter::copy).orElse(null);
        this.status = other.optionalStatus().map(FriendRequestStatusFilter::copy).orElse(null);
        this.createdAt = other.optionalCreatedAt().map(InstantFilter::copy).orElse(null);
        this.respondedAt = other.optionalRespondedAt().map(InstantFilter::copy).orElse(null);
        this.senderId = other.optionalSenderId().map(UUIDFilter::copy).orElse(null);
        this.receiverId = other.optionalReceiverId().map(UUIDFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public FriendRequestCriteria copy() {
        return new FriendRequestCriteria(this);
    }

    public UUIDFilter getId() {
        return id;
    }

    public Optional<UUIDFilter> optionalId() {
        return Optional.ofNullable(id);
    }

    public UUIDFilter id() {
        if (id == null) {
            setId(new UUIDFilter());
        }
        return id;
    }

    public void setId(UUIDFilter id) {
        this.id = id;
    }

    public FriendRequestStatusFilter getStatus() {
        return status;
    }

    public Optional<FriendRequestStatusFilter> optionalStatus() {
        return Optional.ofNullable(status);
    }

    public FriendRequestStatusFilter status() {
        if (status == null) {
            setStatus(new FriendRequestStatusFilter());
        }
        return status;
    }

    public void setStatus(FriendRequestStatusFilter status) {
        this.status = status;
    }

    public InstantFilter getCreatedAt() {
        return createdAt;
    }

    public Optional<InstantFilter> optionalCreatedAt() {
        return Optional.ofNullable(createdAt);
    }

    public InstantFilter createdAt() {
        if (createdAt == null) {
            setCreatedAt(new InstantFilter());
        }
        return createdAt;
    }

    public void setCreatedAt(InstantFilter createdAt) {
        this.createdAt = createdAt;
    }

    public InstantFilter getRespondedAt() {
        return respondedAt;
    }

    public Optional<InstantFilter> optionalRespondedAt() {
        return Optional.ofNullable(respondedAt);
    }

    public InstantFilter respondedAt() {
        if (respondedAt == null) {
            setRespondedAt(new InstantFilter());
        }
        return respondedAt;
    }

    public void setRespondedAt(InstantFilter respondedAt) {
        this.respondedAt = respondedAt;
    }

    public UUIDFilter getSenderId() {
        return senderId;
    }

    public Optional<UUIDFilter> optionalSenderId() {
        return Optional.ofNullable(senderId);
    }

    public UUIDFilter senderId() {
        if (senderId == null) {
            setSenderId(new UUIDFilter());
        }
        return senderId;
    }

    public void setSenderId(UUIDFilter senderId) {
        this.senderId = senderId;
    }

    public UUIDFilter getReceiverId() {
        return receiverId;
    }

    public Optional<UUIDFilter> optionalReceiverId() {
        return Optional.ofNullable(receiverId);
    }

    public UUIDFilter receiverId() {
        if (receiverId == null) {
            setReceiverId(new UUIDFilter());
        }
        return receiverId;
    }

    public void setReceiverId(UUIDFilter receiverId) {
        this.receiverId = receiverId;
    }

    public Boolean getDistinct() {
        return distinct;
    }

    public Optional<Boolean> optionalDistinct() {
        return Optional.ofNullable(distinct);
    }

    public Boolean distinct() {
        if (distinct == null) {
            setDistinct(true);
        }
        return distinct;
    }

    public void setDistinct(Boolean distinct) {
        this.distinct = distinct;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final FriendRequestCriteria that = (FriendRequestCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(status, that.status) &&
            Objects.equals(createdAt, that.createdAt) &&
            Objects.equals(respondedAt, that.respondedAt) &&
            Objects.equals(senderId, that.senderId) &&
            Objects.equals(receiverId, that.receiverId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, status, createdAt, respondedAt, senderId, receiverId, distinct);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "FriendRequestCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalStatus().map(f -> "status=" + f + ", ").orElse("") +
            optionalCreatedAt().map(f -> "createdAt=" + f + ", ").orElse("") +
            optionalRespondedAt().map(f -> "respondedAt=" + f + ", ").orElse("") +
            optionalSenderId().map(f -> "senderId=" + f + ", ").orElse("") +
            optionalReceiverId().map(f -> "receiverId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
