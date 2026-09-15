package com.minh.fakebook.comment.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * A PostCache.
 */
@Entity
@Table(name = "post_cache")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class PostCache implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "id", length = 36)
    private UUID id;

    @NotNull
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "author_id", length = 36, nullable = false)
    private UUID authorId;

    @NotNull
    @Column(name = "visibility", nullable = false)
    private String visibility;

    @NotNull
    @Column(name = "status", nullable = false)
    private String status;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public PostCache id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getAuthorId() {
        return this.authorId;
    }

    public PostCache authorId(UUID authorId) {
        this.setAuthorId(authorId);
        return this;
    }

    public void setAuthorId(UUID authorId) {
        this.authorId = authorId;
    }

    public String getVisibility() {
        return this.visibility;
    }

    public PostCache visibility(String visibility) {
        this.setVisibility(visibility);
        return this;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public String getStatus() {
        return this.status;
    }

    public PostCache status(String status) {
        this.setStatus(status);
        return this;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PostCache)) {
            return false;
        }
        return getId() != null && getId().equals(((PostCache) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "PostCache{" +
            "id=" + getId() +
            ", authorId='" + getAuthorId() + "'" +
            ", visibility='" + getVisibility() + "'" +
            ", status='" + getStatus() + "'" +
            "}";
    }
}
