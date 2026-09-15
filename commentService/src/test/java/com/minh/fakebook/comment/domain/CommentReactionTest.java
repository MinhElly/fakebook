package com.minh.fakebook.comment.domain;

import static com.minh.fakebook.comment.domain.CommentReactionTestSamples.*;
import static com.minh.fakebook.comment.domain.CommentTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.minh.fakebook.comment.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class CommentReactionTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(CommentReaction.class);
        CommentReaction commentReaction1 = getCommentReactionSample1();
        CommentReaction commentReaction2 = new CommentReaction();
        assertThat(commentReaction1).isNotEqualTo(commentReaction2);

        commentReaction2.setId(commentReaction1.getId());
        assertThat(commentReaction1).isEqualTo(commentReaction2);

        commentReaction2 = getCommentReactionSample2();
        assertThat(commentReaction1).isNotEqualTo(commentReaction2);
    }

    @Test
    void commentTest() {
        CommentReaction commentReaction = getCommentReactionRandomSampleGenerator();
        Comment commentBack = getCommentRandomSampleGenerator();

        commentReaction.setComment(commentBack);
        assertThat(commentReaction.getComment()).isEqualTo(commentBack);

        commentReaction.comment(null);
        assertThat(commentReaction.getComment()).isNull();
    }
}
