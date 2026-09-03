package com.minh.fakebook.user.service.criteria;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link com.minh.fakebook.user.domain.Follow} entity. This class is used
 * in {@link com.minh.fakebook.user.web.rest.FollowResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /follows?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class FollowCriteria implements Serializable, Criteria {

    @Serial
    private static final long serialVersionUID = 1L;

    private UUIDFilter id;

    private InstantFilter createdAt;

    private UUIDFilter followerId;

    private UUIDFilter followingId;

    private Boolean distinct;

    public FollowCriteria() {}

    public FollowCriteria(FollowCriteria other) {
        this.id = other.optionalId().map(UUIDFilter::copy).orElse(null);
        this.createdAt = other.optionalCreatedAt().map(InstantFilter::copy).orElse(null);
        this.followerId = other.optionalFollowerId().map(UUIDFilter::copy).orElse(null);
        this.followingId = other.optionalFollowingId().map(UUIDFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public FollowCriteria copy() {
        return new FollowCriteria(this);
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

    public UUIDFilter getFollowerId() {
        return followerId;
    }

    public Optional<UUIDFilter> optionalFollowerId() {
        return Optional.ofNullable(followerId);
    }

    public UUIDFilter followerId() {
        if (followerId == null) {
            setFollowerId(new UUIDFilter());
        }
        return followerId;
    }

    public void setFollowerId(UUIDFilter followerId) {
        this.followerId = followerId;
    }

    public UUIDFilter getFollowingId() {
        return followingId;
    }

    public Optional<UUIDFilter> optionalFollowingId() {
        return Optional.ofNullable(followingId);
    }

    public UUIDFilter followingId() {
        if (followingId == null) {
            setFollowingId(new UUIDFilter());
        }
        return followingId;
    }

    public void setFollowingId(UUIDFilter followingId) {
        this.followingId = followingId;
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
        final FollowCriteria that = (FollowCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(createdAt, that.createdAt) &&
            Objects.equals(followerId, that.followerId) &&
            Objects.equals(followingId, that.followingId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, createdAt, followerId, followingId, distinct);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "FollowCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalCreatedAt().map(f -> "createdAt=" + f + ", ").orElse("") +
            optionalFollowerId().map(f -> "followerId=" + f + ", ").orElse("") +
            optionalFollowingId().map(f -> "followingId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
