package com.minh.fakebook.user.domain;

import java.util.UUID;

public class UserProfileTestSamples {

    public static UserProfile getUserProfileSample1() {
        return new UserProfile()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .username("username1")
            .displayName("displayName1")
            .avatarMediaId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .coverMediaId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"));
    }

    public static UserProfile getUserProfileSample2() {
        return new UserProfile()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .username("username2")
            .displayName("displayName2")
            .avatarMediaId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .coverMediaId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"));
    }

    public static UserProfile getUserProfileRandomSampleGenerator() {
        return new UserProfile()
            .id(UUID.randomUUID())
            .username(UUID.randomUUID().toString())
            .displayName(UUID.randomUUID().toString())
            .avatarMediaId(UUID.randomUUID())
            .coverMediaId(UUID.randomUUID());
    }
}
