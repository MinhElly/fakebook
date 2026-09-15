package com.minh.fakebook.comment.domain;

import java.util.UUID;

public class PostCacheTestSamples {

    public static PostCache getPostCacheSample1() {
        return new PostCache()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .authorId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .visibility("visibility1")
            .status("status1");
    }

    public static PostCache getPostCacheSample2() {
        return new PostCache()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .authorId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .visibility("visibility2")
            .status("status2");
    }

    public static PostCache getPostCacheRandomSampleGenerator() {
        return new PostCache()
            .id(UUID.randomUUID())
            .authorId(UUID.randomUUID())
            .visibility(UUID.randomUUID().toString())
            .status(UUID.randomUUID().toString());
    }
}
