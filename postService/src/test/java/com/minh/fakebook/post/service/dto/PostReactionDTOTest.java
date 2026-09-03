package com.minh.fakebook.post.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.minh.fakebook.post.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PostReactionDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(PostReactionDTO.class);
        PostReactionDTO postReactionDTO1 = new PostReactionDTO();
        postReactionDTO1.setId(UUID.randomUUID());
        PostReactionDTO postReactionDTO2 = new PostReactionDTO();
        assertThat(postReactionDTO1).isNotEqualTo(postReactionDTO2);
        postReactionDTO2.setId(postReactionDTO1.getId());
        assertThat(postReactionDTO1).isEqualTo(postReactionDTO2);
        postReactionDTO2.setId(UUID.randomUUID());
        assertThat(postReactionDTO1).isNotEqualTo(postReactionDTO2);
        postReactionDTO1.setId(null);
        assertThat(postReactionDTO1).isNotEqualTo(postReactionDTO2);
    }
}
