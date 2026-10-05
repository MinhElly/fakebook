package com.minh.fakebook.comment.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.minh.fakebook.comment.IntegrationTest;
import com.minh.fakebook.comment.domain.Comment;
import com.minh.fakebook.comment.domain.CommentReaction;
import com.minh.fakebook.comment.domain.enumeration.CommentStatus;
import com.minh.fakebook.comment.domain.enumeration.ReactionType;
import com.minh.fakebook.comment.repository.CommentReactionRepository;
import com.minh.fakebook.comment.repository.CommentRepository;
import com.minh.fakebook.comment.security.AuthoritiesConstants;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** Integration tests for the {@link CommentReactionResource} REST controller. */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser(authorities = AuthoritiesConstants.ADMIN)
class CommentReactionResourceIT {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID POST_ID = UUID.randomUUID();
    private static final UUID AUTHOR_ID = UUID.randomUUID();
    private static final String ENTITY_API_URL = "/api/comment-reactions";

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private CommentReactionRepository commentReactionRepository;

    @Autowired
    private MockMvc restCommentReactionMockMvc;

    private Comment comment;

    @BeforeEach
    void initTest() {
        comment = new Comment()
            .postId(POST_ID)
            .authorId(AUTHOR_ID)
            .content("A comment")
            .status(CommentStatus.ACTIVE);
    }

    @Test
    @Transactional
    void adminCanGetAllCommentReactions() throws Exception {
        comment = commentRepository.saveAndFlush(comment);
        CommentReaction reaction = saveReaction(USER_ID, ReactionType.LIKE);

        restCommentReactionMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(reaction.getId().toString())))
            .andExpect(jsonPath("$.[*].userId").value(hasItem(USER_ID.toString())))
            .andExpect(jsonPath("$.[*].reactionType").value(hasItem(ReactionType.LIKE.toString())));
    }

    @Test
    @Transactional
    void adminCanFilterAndCountCommentReactions() throws Exception {
        comment = commentRepository.saveAndFlush(comment);
        CommentReaction reaction = saveReaction(USER_ID, ReactionType.LOVE);

        restCommentReactionMockMvc
            .perform(get(ENTITY_API_URL + "?userId.equals=" + USER_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.[*].id").value(hasItem(reaction.getId().toString())));

        restCommentReactionMockMvc
            .perform(get(ENTITY_API_URL + "/count?userId.equals=" + USER_ID))
            .andExpect(status().isOk())
            .andExpect(content().string("1"));
    }

    @Test
    @Transactional
    void adminCanGetCommentReaction() throws Exception {
        comment = commentRepository.saveAndFlush(comment);
        CommentReaction reaction = saveReaction(USER_ID, ReactionType.LIKE);

        restCommentReactionMockMvc
            .perform(get(ENTITY_API_URL + "/{id}", reaction.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(reaction.getId().toString()))
            .andExpect(jsonPath("$.comment.id").value(comment.getId().toString()));
    }

    @Test
    @Transactional
    void getNonExistingCommentReactionReturnsNotFound() throws Exception {
        restCommentReactionMockMvc.perform(get(ENTITY_API_URL + "/{id}", UUID.randomUUID())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    @WithMockUser(authorities = AuthoritiesConstants.USER)
    void regularUserCannotUseAdminReadEndpoints() throws Exception {
        restCommentReactionMockMvc.perform(get(ENTITY_API_URL)).andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    void toggleCreatesReactionForAuthenticatedUser() throws Exception {
        comment = commentRepository.saveAndFlush(comment);

        restCommentReactionMockMvc
            .perform(
                post(ENTITY_API_URL + "/toggle")
                    .with(jwt().jwt(token -> token.subject(USER_ID.toString())))
                    .with(csrf())
                    .param("commentId", comment.getId().toString())
                    .param("reactionType", ReactionType.LIKE.toString())
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userId").value(USER_ID.toString()))
            .andExpect(jsonPath("$.reactionType").value(ReactionType.LIKE.toString()))
            .andExpect(jsonPath("$.comment.id").value(comment.getId().toString()));

        assertThat(commentReactionRepository.findByCommentIdAndUserId(comment.getId(), USER_ID)).isPresent();
    }

    @Test
    @Transactional
    void toggleWithSameTypeRemovesReaction() throws Exception {
        comment = commentRepository.saveAndFlush(comment);
        saveReaction(USER_ID, ReactionType.LIKE);

        restCommentReactionMockMvc
            .perform(
                post(ENTITY_API_URL + "/toggle")
                    .with(jwt().jwt(token -> token.subject(USER_ID.toString())))
                    .with(csrf())
                    .param("commentId", comment.getId().toString())
                    .param("reactionType", ReactionType.LIKE.toString())
            )
            .andExpect(status().isNoContent());

        assertThat(commentReactionRepository.findByCommentIdAndUserId(comment.getId(), USER_ID)).isEmpty();
    }

    @Test
    @Transactional
    void toggleWithDifferentTypeUpdatesReaction() throws Exception {
        comment = commentRepository.saveAndFlush(comment);
        saveReaction(USER_ID, ReactionType.LIKE);

        restCommentReactionMockMvc
            .perform(
                post(ENTITY_API_URL + "/toggle")
                    .with(jwt().jwt(token -> token.subject(USER_ID.toString())))
                    .with(csrf())
                    .param("commentId", comment.getId().toString())
                    .param("reactionType", ReactionType.LOVE.toString())
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.reactionType").value(ReactionType.LOVE.toString()));

        CommentReaction updated = commentReactionRepository.findByCommentIdAndUserId(comment.getId(), USER_ID).orElseThrow();
        assertThat(updated.getReactionType()).isEqualTo(ReactionType.LOVE);
        assertThat(updated.getUpdatedAt()).isNotNull();
    }

    private CommentReaction saveReaction(UUID userId, ReactionType reactionType) {
        return commentReactionRepository.saveAndFlush(
            new CommentReaction().userId(userId).reactionType(reactionType).createdAt(Instant.now()).comment(comment)
        );
    }
}
