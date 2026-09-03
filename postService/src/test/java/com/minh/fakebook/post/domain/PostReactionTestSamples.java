package com.minh.fakebook.post.domain;

import java.util.UUID;

public class PostReactionTestSamples {

    public static PostReaction getPostReactionSample1() {
        return new PostReaction()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .userId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"));
    }

    public static PostReaction getPostReactionSample2() {
        return new PostReaction()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .userId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"));
    }

    public static PostReaction getPostReactionRandomSampleGenerator() {
        return new PostReaction().id(UUID.randomUUID()).userId(UUID.randomUUID());
    }
}
