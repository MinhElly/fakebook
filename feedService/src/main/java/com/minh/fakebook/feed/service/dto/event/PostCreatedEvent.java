package com.minh.fakebook.feed.service.dto.event;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

public class PostCreatedEvent implements Serializable {

    @JsonProperty("id")
    @JsonAlias({"id", "postId"})
    private UUID postId;

    private UUID authorId;
    private String visibility;
    private Instant createdAt;

    public PostCreatedEvent() {
    }

    public PostCreatedEvent(UUID postId, UUID authorId, String visibility, Instant createdAt) {
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

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
