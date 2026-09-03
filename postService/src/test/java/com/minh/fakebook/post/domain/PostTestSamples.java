package com.minh.fakebook.post.domain;

import java.util.UUID;

public class PostTestSamples {

    public static Post getPostSample1() {
        return new Post()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .authorId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"));
    }

    public static Post getPostSample2() {
        return new Post()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .authorId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"));
    }

    public static Post getPostRandomSampleGenerator() {
        return new Post().id(UUID.randomUUID()).authorId(UUID.randomUUID());
    }
}
