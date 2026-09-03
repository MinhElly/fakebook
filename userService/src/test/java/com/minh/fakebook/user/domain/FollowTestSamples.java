package com.minh.fakebook.user.domain;

import java.util.UUID;

public class FollowTestSamples {

    public static Follow getFollowSample1() {
        return new Follow().id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"));
    }

    public static Follow getFollowSample2() {
        return new Follow().id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"));
    }

    public static Follow getFollowRandomSampleGenerator() {
        return new Follow().id(UUID.randomUUID());
    }
}
