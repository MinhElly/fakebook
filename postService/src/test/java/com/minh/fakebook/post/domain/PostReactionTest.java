package com.minh.fakebook.post.domain;

import static com.minh.fakebook.post.domain.PostReactionTestSamples.*;
import static com.minh.fakebook.post.domain.PostTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.minh.fakebook.post.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class PostReactionTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(PostReaction.class);
        PostReaction postReaction1 = getPostReactionSample1();
        PostReaction postReaction2 = new PostReaction();
        assertThat(postReaction1).isNotEqualTo(postReaction2);

        postReaction2.setId(postReaction1.getId());
        assertThat(postReaction1).isEqualTo(postReaction2);

        postReaction2 = getPostReactionSample2();
        assertThat(postReaction1).isNotEqualTo(postReaction2);
    }

    @Test
    void postTest() {
        PostReaction postReaction = getPostReactionRandomSampleGenerator();
        Post postBack = getPostRandomSampleGenerator();

        postReaction.setPost(postBack);
        assertThat(postReaction.getPost()).isEqualTo(postBack);

        postReaction.post(null);
        assertThat(postReaction.getPost()).isNull();
    }
}
