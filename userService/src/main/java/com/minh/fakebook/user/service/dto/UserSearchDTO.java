package com.minh.fakebook.user.service.dto;

import java.io.Serializable;
import java.util.UUID;

public class UserSearchDTO implements Serializable {
    private UUID id;
    private String username;
    private String displayName;
    private UUID avatarMediaId;
    private String bio;
    private String education;
    private String workplace;
    private String location;
    private long mutualFriendsCount;
    private String friendshipStatus;

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
}
