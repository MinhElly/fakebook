package com.minh.fakebook.user.domain;

import java.util.UUID;

public class FriendshipTestSamples {

    public static Friendship getFriendshipSample1() {
        return new Friendship().id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"));
    }

    public static Friendship getFriendshipSample2() {
        return new Friendship().id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"));
    }

    public static Friendship getFriendshipRandomSampleGenerator() {
        return new Friendship().id(UUID.randomUUID());
    }
}
