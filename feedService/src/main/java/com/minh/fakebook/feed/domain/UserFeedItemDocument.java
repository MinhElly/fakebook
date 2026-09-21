package com.minh.fakebook.feed.domain;

import org.springframework.data.mongodb.core.mapping.FieldType;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import java.io.Serializable;
import java.util.UUID;
import java.time.Instant;

/**
 * MongoDB Document for storing denormalized feed items (NoSQL Read Model).
 */
@Document(collection = "user_feed_items")
public class UserFeedItemDocument implements Serializable {

    @Id
    private String id;

    @Field(value = "user_id", targetType = FieldType.STRING)
    private UUID userId;

    @Field(value = "post_id", targetType = FieldType.STRING)
    private UUID postId;

    @Field("created_at")
    private Instant createdAt;

    /** Gets id. @return id */
    public String getId() {
        return id;
    }

    /** Sets id. @param id the id */
    public void setId(String id) {
        this.id = id;
    }

    /** Gets userId. @return userId */
    public UUID getUserId() {
        return userId;
    }

    /** Sets userId. @param userId the userId */
    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    /** Gets postId. @return postId */
    public UUID getPostId() {
        return postId;
    }

    /** Sets postId. @param postId the postId */
    public void setPostId(UUID postId) {
        this.postId = postId;
    }

    /** Gets createdAt. @return createdAt */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /** Sets createdAt. @param createdAt the createdAt */
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
