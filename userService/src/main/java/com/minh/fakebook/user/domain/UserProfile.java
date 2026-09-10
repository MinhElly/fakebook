package com.minh.fakebook.user.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * A UserProfile.
 */
@Entity
@Table(name = "user_profiles")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class UserProfile implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "id", length = 36)
    private UUID id;

    @NotNull
    @Size(max = 50)
    @Column(name = "username", length = 50, nullable = false, unique = true)
    private String username;

    @NotNull
    @Size(max = 100)
    @Column(name = "display_name", length = 100, nullable = false)
    private String displayName;

    @Lob
    @Column(name = "bio")
    private String bio;

    @Column(name = "birthday")
    private LocalDate birthday;

    @Size(max = 20)
    @Column(name = "gender", length = 20)
    private String gender;

    @Size(max = 255)
    @Column(name = "location", length = 255)
    private String location;

    @Size(max = 255)
    @Column(name = "education", length = 255)
    private String education;

    @Size(max = 255)
    @Column(name = "work", length = 255)
    private String work;

    @Size(max = 50)
    @Column(name = "relationship", length = 50)
    private String relationship;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "avatar_media_id", length = 36)
    private UUID avatarMediaId;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "cover_media_id", length = 36)
    private UUID coverMediaId;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public UserProfile id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getUsername() {
        return this.username;
    }

    public UserProfile username(String username) {
        this.setUsername(username);
        return this;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public UserProfile displayName(String displayName) {
        this.setDisplayName(displayName);
        return this;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getBio() {
        return this.bio;
    }

    public UserProfile bio(String bio) {
        this.setBio(bio);
        return this;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public LocalDate getBirthday() {
        return this.birthday;
    }

    public UserProfile birthday(LocalDate birthday) {
        this.setBirthday(birthday);
        return this;
    }

    public void setBirthday(LocalDate birthday) {
        this.birthday = birthday;
    }

    public String getGender() {
        return this.gender;
    }

    public UserProfile gender(String gender) {
        this.setGender(gender);
        return this;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getLocation() {
        return this.location;
    }

    public UserProfile location(String location) {
        this.setLocation(location);
        return this;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getEducation() {
        return this.education;
    }

    public UserProfile education(String education) {
        this.setEducation(education);
        return this;
    }

    public void setEducation(String education) {
        this.education = education;
    }

    public String getWork() {
        return this.work;
    }

    public UserProfile work(String work) {
        this.setWork(work);
        return this;
    }

    public void setWork(String work) {
        this.work = work;
    }

    public String getRelationship() {
        return this.relationship;
    }

    public UserProfile relationship(String relationship) {
        this.setRelationship(relationship);
        return this;
    }

    public void setRelationship(String relationship) {
        this.relationship = relationship;
    }

    public UUID getAvatarMediaId() {
        return this.avatarMediaId;
    }

    public UserProfile avatarMediaId(UUID avatarMediaId) {
        this.setAvatarMediaId(avatarMediaId);
        return this;
    }

    public void setAvatarMediaId(UUID avatarMediaId) {
        this.avatarMediaId = avatarMediaId;
    }

    public UUID getCoverMediaId() {
        return this.coverMediaId;
    }

    public UserProfile coverMediaId(UUID coverMediaId) {
        this.setCoverMediaId(coverMediaId);
        return this;
    }

    public void setCoverMediaId(UUID coverMediaId) {
        this.coverMediaId = coverMediaId;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }

    public UserProfile createdAt(Instant createdAt) {
        this.setCreatedAt(createdAt);
        return this;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return this.updatedAt;
    }

    public UserProfile updatedAt(Instant updatedAt) {
        this.setUpdatedAt(updatedAt);
        return this;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UserProfile)) {
            return false;
        }
        return getId() != null && getId().equals(((UserProfile) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "UserProfile{" +
            "id=" + getId() +
            ", username='" + getUsername() + "'" +
            ", displayName='" + getDisplayName() + "'" +
            ", bio='" + getBio() + "'" +
            ", avatarMediaId='" + getAvatarMediaId() + "'" +
            ", coverMediaId='" + getCoverMediaId() + "'" +
            ", createdAt='" + getCreatedAt() + "'" +
            ", updatedAt='" + getUpdatedAt() + "'" +
            "}";
    }
}
