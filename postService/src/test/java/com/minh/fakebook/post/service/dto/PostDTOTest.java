package com.minh.fakebook.post.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.minh.fakebook.post.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PostDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(PostDTO.class);
        PostDTO postDTO1 = new PostDTO();
        postDTO1.setId(UUID.randomUUID());
        PostDTO postDTO2 = new PostDTO();
        assertThat(postDTO1).isNotEqualTo(postDTO2);
        postDTO2.setId(postDTO1.getId());
        assertThat(postDTO1).isEqualTo(postDTO2);
        postDTO2.setId(UUID.randomUUID());
        assertThat(postDTO1).isNotEqualTo(postDTO2);
        postDTO1.setId(null);
        assertThat(postDTO1).isNotEqualTo(postDTO2);
    }
}
