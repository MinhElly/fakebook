package com.minh.fakebook.user.service.dto;

import java.util.UUID;

public class UserProfileDetailDTO {
    private UUID id;
    private String username;
    private String displayName;
    private UUID avatarMediaId;
    private UUID coverMediaId;
    private String bio;
    private String education;
    private String workplace;
    private String location;
    private String relationship;
    private long mutualFriendsCount;
    private String friendshipStatus;
    private UUID friendRequestId;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public UUID getAvatarMediaId() {
        return avatarMediaId;
    }

    public void setAvatarMediaId(UUID avatarMediaId) {
        this.avatarMediaId = avatarMediaId;
    }

    public UUID getCoverMediaId() {
        return coverMediaId;
    }

    public void setCoverMediaId(UUID coverMediaId) {
        this.coverMediaId = coverMediaId;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getEducation() {
        return education;
    }

    public void setEducation(String education) {
        this.education = education;
    }

    public String getWorkplace() {
        return workplace;
    }

    public void setWorkplace(String workplace) {
        this.workplace = workplace;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getRelationship() {
        return relationship;
    }

    public void setRelationship(String relationship) {
        this.relationship = relationship;
    }

    public long getMutualFriendsCount() {
        return mutualFriendsCount;
    }

    public void setMutualFriendsCount(long mutualFriendsCount) {
        this.mutualFriendsCount = mutualFriendsCount;
    }

    public String getFriendshipStatus() {
        return friendshipStatus;
    }

    public void setFriendshipStatus(String friendshipStatus) {
        this.friendshipStatus = friendshipStatus;
    }

    public UUID getFriendRequestId() {
        return friendRequestId;
    }

    public void setFriendRequestId(UUID friendRequestId) {
        this.friendRequestId = friendRequestId;
    }
}
