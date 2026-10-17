package com.minh.fakebook.post.domain;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Entity representing a post saved by a user.
 */
@org.hibernate.annotations.Filter(name="community",deduceAliasInjectionPoints=false,
    aliases=@org.hibernate.annotations.SqlFragmentAlias(alias="self",table="saved_post"),
    condition="(:administrationView = true or ({self}.user_id not in (:excludedSubjects) and {self}.community_archived=false and {self}.post_id in (select p.id from posts p where p.status='ACTIVE' and p.moderation_visibility='VISIBLE' and p.community_archived=false and p.author_id not in (:excludedSubjects))))")
@Entity
@Table(name = "saved_post")
public class SavedPost implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "post_id", nullable = false)
    private UUID postId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /**
     * @return the id
     */
    public UUID getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(UUID id) {
        this.id = id;
    }

    /**
     * @return the userId
     */
    public UUID getUserId() {
        return userId;
    }

    /**
     * @param userId the userId to set
     */
    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    /**
     * @return the postId
     */
    public UUID getPostId() {
        return postId;
    }

    /**
     * @param postId the postId to set
     */
    public void setPostId(UUID postId) {
        this.postId = postId;
    }

    /**
     * @return the createdAt
     */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * @param createdAt the createdAt to set
     */
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
