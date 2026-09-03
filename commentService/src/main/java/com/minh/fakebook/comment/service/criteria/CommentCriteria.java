package com.minh.fakebook.comment.service.criteria;

import com.minh.fakebook.comment.domain.enumeration.CommentStatus;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link com.minh.fakebook.comment.domain.Comment} entity. This class is used
 * in {@link com.minh.fakebook.comment.web.rest.CommentResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /comments?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class CommentCriteria implements Serializable, Criteria {

    /**
     * Class for filtering CommentStatus
     */
    public static class CommentStatusFilter extends Filter<CommentStatus> {

        public CommentStatusFilter() {}

        public CommentStatusFilter(CommentStatusFilter filter) {
            super(filter);
        }

        @Override
        public CommentStatusFilter copy() {
            return new CommentStatusFilter(this);
        }
    }

    @Serial
    private static final long serialVersionUID = 1L;

    private UUIDFilter id;

    private UUIDFilter postId;

    private UUIDFilter authorId;

    private CommentStatusFilter status;

    private UUIDFilter parentCommentId;

    private Boolean distinct;

    public CommentCriteria() {}

    public CommentCriteria(CommentCriteria other) {
        this.id = other.optionalId().map(UUIDFilter::copy).orElse(null);
        this.postId = other.optionalPostId().map(UUIDFilter::copy).orElse(null);
        this.authorId = other.optionalAuthorId().map(UUIDFilter::copy).orElse(null);
        this.status = other.optionalStatus().map(CommentStatusFilter::copy).orElse(null);
        this.parentCommentId = other.optionalParentCommentId().map(UUIDFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public CommentCriteria copy() {
        return new CommentCriteria(this);
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

    public UUIDFilter getPostId() {
        return postId;
    }

    public Optional<UUIDFilter> optionalPostId() {
        return Optional.ofNullable(postId);
    }

    public UUIDFilter postId() {
        if (postId == null) {
            setPostId(new UUIDFilter());
        }
        return postId;
    }

    public void setPostId(UUIDFilter postId) {
        this.postId = postId;
    }

    public UUIDFilter getAuthorId() {
        return authorId;
    }

    public Optional<UUIDFilter> optionalAuthorId() {
        return Optional.ofNullable(authorId);
    }

    public UUIDFilter authorId() {
        if (authorId == null) {
            setAuthorId(new UUIDFilter());
        }
        return authorId;
    }

    public void setAuthorId(UUIDFilter authorId) {
        this.authorId = authorId;
    }

    public CommentStatusFilter getStatus() {
        return status;
    }

    public Optional<CommentStatusFilter> optionalStatus() {
        return Optional.ofNullable(status);
    }

    public CommentStatusFilter status() {
        if (status == null) {
            setStatus(new CommentStatusFilter());
        }
        return status;
    }

    public void setStatus(CommentStatusFilter status) {
        this.status = status;
    }

    public UUIDFilter getParentCommentId() {
        return parentCommentId;
    }

    public Optional<UUIDFilter> optionalParentCommentId() {
        return Optional.ofNullable(parentCommentId);
    }

    public UUIDFilter parentCommentId() {
        if (parentCommentId == null) {
            setParentCommentId(new UUIDFilter());
        }
        return parentCommentId;
    }

    public void setParentCommentId(UUIDFilter parentCommentId) {
        this.parentCommentId = parentCommentId;
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
        final CommentCriteria that = (CommentCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(postId, that.postId) &&
            Objects.equals(authorId, that.authorId) &&
            Objects.equals(status, that.status) &&
            Objects.equals(parentCommentId, that.parentCommentId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, postId, authorId, status, parentCommentId, distinct);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "CommentCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalPostId().map(f -> "postId=" + f + ", ").orElse("") +
            optionalAuthorId().map(f -> "authorId=" + f + ", ").orElse("") +
            optionalStatus().map(f -> "status=" + f + ", ").orElse("") +
            optionalParentCommentId().map(f -> "parentCommentId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
