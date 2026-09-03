package com.minh.fakebook.comment.domain;

import static com.minh.fakebook.comment.domain.CommentTestSamples.*;
import static com.minh.fakebook.comment.domain.CommentTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.minh.fakebook.comment.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class CommentTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Comment.class);
        Comment comment1 = getCommentSample1();
        Comment comment2 = new Comment();
        assertThat(comment1).isNotEqualTo(comment2);

        comment2.setId(comment1.getId());
        assertThat(comment1).isEqualTo(comment2);

        comment2 = getCommentSample2();
        assertThat(comment1).isNotEqualTo(comment2);
    }

    @Test
    void parentCommentTest() {
        Comment comment = getCommentRandomSampleGenerator();
        Comment commentBack = getCommentRandomSampleGenerator();

        comment.setParentComment(commentBack);
        assertThat(comment.getParentComment()).isEqualTo(commentBack);

        comment.parentComment(null);
        assertThat(comment.getParentComment()).isNull();
    }
}
