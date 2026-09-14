package com.minh.fakebook.comment.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.minh.fakebook.comment.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CommentReactionDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(CommentReactionDTO.class);
        CommentReactionDTO commentReactionDTO1 = new CommentReactionDTO();
        commentReactionDTO1.setId(UUID.randomUUID());
        CommentReactionDTO commentReactionDTO2 = new CommentReactionDTO();
        assertThat(commentReactionDTO1).isNotEqualTo(commentReactionDTO2);
        commentReactionDTO2.setId(commentReactionDTO1.getId());
        assertThat(commentReactionDTO1).isEqualTo(commentReactionDTO2);
        commentReactionDTO2.setId(UUID.randomUUID());
        assertThat(commentReactionDTO1).isNotEqualTo(commentReactionDTO2);
        commentReactionDTO1.setId(null);
        assertThat(commentReactionDTO1).isNotEqualTo(commentReactionDTO2);
    }
}
