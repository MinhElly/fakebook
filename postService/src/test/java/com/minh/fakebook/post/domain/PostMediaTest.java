package com.minh.fakebook.post.domain;

import static com.minh.fakebook.post.domain.PostMediaTestSamples.*;
import static com.minh.fakebook.post.domain.PostTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.minh.fakebook.post.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class PostMediaTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(PostMedia.class);
        PostMedia postMedia1 = getPostMediaSample1();
        PostMedia postMedia2 = new PostMedia();
        assertThat(postMedia1).isNotEqualTo(postMedia2);

        postMedia2.setId(postMedia1.getId());
        assertThat(postMedia1).isEqualTo(postMedia2);

        postMedia2 = getPostMediaSample2();
        assertThat(postMedia1).isNotEqualTo(postMedia2);
    }

    @Test
    void postTest() {
        PostMedia postMedia = getPostMediaRandomSampleGenerator();
        Post postBack = getPostRandomSampleGenerator();

        postMedia.setPost(postBack);
        assertThat(postMedia.getPost()).isEqualTo(postBack);

        postMedia.post(null);
        assertThat(postMedia.getPost()).isNull();
    }
}
