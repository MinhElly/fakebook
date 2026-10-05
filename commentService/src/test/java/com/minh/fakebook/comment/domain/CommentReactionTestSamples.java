package com.minh.fakebook.comment.domain;

import java.util.UUID;

public class CommentReactionTestSamples {

    public static CommentReaction getCommentReactionSample1() {
        return new CommentReaction()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .userId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"));
    }

    public static CommentReaction getCommentReactionSample2() {
        return new CommentReaction()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .userId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"));
    }

    public static CommentReaction getCommentReactionRandomSampleGenerator() {
        return new CommentReaction().id(UUID.randomUUID()).userId(UUID.randomUUID());
    }
}
