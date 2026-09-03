package com.minh.fakebook.user.domain;

import java.util.UUID;

public class FriendRequestTestSamples {

    public static FriendRequest getFriendRequestSample1() {
        return new FriendRequest().id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"));
    }

    public static FriendRequest getFriendRequestSample2() {
        return new FriendRequest().id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"));
    }

    public static FriendRequest getFriendRequestRandomSampleGenerator() {
        return new FriendRequest().id(UUID.randomUUID());
    }
}
