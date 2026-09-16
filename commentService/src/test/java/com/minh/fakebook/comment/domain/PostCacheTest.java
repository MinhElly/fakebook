package com.minh.fakebook.comment.domain;

import static com.minh.fakebook.comment.domain.PostCacheTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.minh.fakebook.comment.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class PostCacheTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(PostCache.class);
        PostCache postCache1 = getPostCacheSample1();
        PostCache postCache2 = new PostCache();
        assertThat(postCache1).isNotEqualTo(postCache2);

        postCache2.setId(postCache1.getId());
        assertThat(postCache1).isEqualTo(postCache2);

        postCache2 = getPostCacheSample2();
        assertThat(postCache1).isNotEqualTo(postCache2);
    }
}
