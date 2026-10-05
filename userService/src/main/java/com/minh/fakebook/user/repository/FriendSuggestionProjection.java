package com.minh.fakebook.user.repository;

import java.util.UUID;

public interface FriendSuggestionProjection {

    UUID getUserId();

    long getMutualFriendsCount();
}
