package com.minh.fakebook.comment.domain;

import java.util.UUID;

public class CommentTestSamples {

    public static Comment getCommentSample1() {
        return new Comment()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .postId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .authorId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"));
    }

    public static Comment getCommentSample2() {
        return new Comment()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .postId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .authorId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"));
    }

    public static Comment getCommentRandomSampleGenerator() {
        return new Comment().id(UUID.randomUUID()).postId(UUID.randomUUID()).authorId(UUID.randomUUID());
    }
}
