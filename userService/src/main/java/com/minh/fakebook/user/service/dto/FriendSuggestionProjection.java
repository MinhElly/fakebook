package com.minh.fakebook.user.service.dto;

import java.util.UUID;

public interface FriendSuggestionProjection {

    UUID getUserId();

    long getMutualFriendsCount();
}
