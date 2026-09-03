package com.minh.fakebook.post.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.minh.fakebook.post.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PostMediaDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(PostMediaDTO.class);
        PostMediaDTO postMediaDTO1 = new PostMediaDTO();
        postMediaDTO1.setId(UUID.randomUUID());
        PostMediaDTO postMediaDTO2 = new PostMediaDTO();
        assertThat(postMediaDTO1).isNotEqualTo(postMediaDTO2);
        postMediaDTO2.setId(postMediaDTO1.getId());
        assertThat(postMediaDTO1).isEqualTo(postMediaDTO2);
        postMediaDTO2.setId(UUID.randomUUID());
        assertThat(postMediaDTO1).isNotEqualTo(postMediaDTO2);
        postMediaDTO1.setId(null);
        assertThat(postMediaDTO1).isNotEqualTo(postMediaDTO2);
    }
}
