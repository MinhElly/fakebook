package com.minh.fakebook.post.service.dto.event;

import com.minh.fakebook.post.domain.enumeration.PostVisibility;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

public class PostCreateEvent implements Serializable {
    private UUID postId;
    private UUID authorId;
    private PostVisibility visibility;
    private Instant createdAt;

    public PostCreateEvent() {
    }

    public PostCreateEvent(UUID postId, UUID authorId, PostVisibility visibility, Instant createdAt) {
        this.postId = postId;
        this.authorId = authorId;
        this.visibility = visibility;
        this.createdAt = createdAt;
    }

    public UUID getPostId() {
        return postId;
    }

    public void setPostId(UUID postId) {
        this.postId = postId;
    }

    public UUID getAuthorId() {
        return authorId;
    }

    public void setAuthorId(UUID authorId) {
        this.authorId = authorId;
    }

    public PostVisibility getVisibility() {
        return visibility;
    }

    public void setVisibility(PostVisibility visibility) {
        this.visibility = visibility;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
