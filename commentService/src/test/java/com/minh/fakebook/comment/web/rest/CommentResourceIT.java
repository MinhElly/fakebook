package com.minh.fakebook.comment.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.minh.fakebook.comment.IntegrationTest;
import com.minh.fakebook.comment.domain.Comment;
import com.minh.fakebook.comment.domain.PostCache;
import com.minh.fakebook.comment.domain.enumeration.CommentStatus;
import com.minh.fakebook.comment.repository.CommentRepository;
import com.minh.fakebook.comment.repository.PostCacheRepository;
import com.minh.fakebook.comment.security.AuthoritiesConstants;
import com.minh.fakebook.comment.service.dto.CreateCommentRequestDTO;
import com.minh.fakebook.comment.service.dto.ReplyCommentRequestDTO;
import com.minh.fakebook.comment.service.dto.UpdateCommentRequestDTO;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/** Integration tests for the {@link CommentResource} REST controller. */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser(authorities = AuthoritiesConstants.USER)
class CommentResourceIT {

    private static final UUID POST_ID = UUID.randomUUID();
    private static final UUID AUTHOR_ID = UUID.randomUUID();
    private static final UUID OTHER_USER_ID = UUID.randomUUID();
    private static final String DEFAULT_CONTENT = "A comment";
    private static final String UPDATED_CONTENT = "An updated comment";
    private static final String ENTITY_API_URL = "/api/comments";

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private PostCacheRepository postCacheRepository;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate fixtureJdbc;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private com.minh.fakebook.comment.client.PostFeignClient authoritativePosts;

    @org.junit.jupiter.api.BeforeEach
    void configureAuthoritativePostFixture() {
        org.mockito.Mockito.when(authoritativePosts.getPostById(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
            UUID id = invocation.getArgument(0);
            return postCacheRepository.findById(id)
                .map(p -> new com.minh.fakebook.comment.client.dto.PostSyncDTO(p.getId(), p.getAuthorId(), p.getStatus(), p.getVisibility()))
                .orElseThrow(() -> new IllegalArgumentException("Post fixture not found"));
        });
    }


    @Autowired
    private MockMvc restCommentMockMvc;

    private Comment comment;

    @BeforeEach
    void initTest() {
        if (!postCacheRepository.existsById(POST_ID)) {
            fixtureJdbc.update("INSERT INTO post_cache (id, author_id, visibility, status) VALUES (?, ?, ?, ?)",
                POST_ID.toString(), AUTHOR_ID.toString(), "PUBLIC", "ACTIVE");
        }
        comment = new Comment()
            .postId(POST_ID)
            .authorId(AUTHOR_ID)
            .content(DEFAULT_CONTENT)
            .status(CommentStatus.ACTIVE);
    }

    @Test
    @Transactional
    void createCommentUsesAuthenticatedUserAsAuthor() throws Exception {
        PostCache post = postCacheRepository.saveAndFlush(
            new PostCache().authorId(AUTHOR_ID).visibility("PUBLIC").status("ACTIVE")
        );
        long countBefore = commentRepository.count();
        CreateCommentRequestDTO request = new CreateCommentRequestDTO(post.getId(), DEFAULT_CONTENT);

        restCommentMockMvc
            .perform(
                post(ENTITY_API_URL + "/create")
                    .with(jwt().jwt(token -> token.subject(AUTHOR_ID.toString())))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(request))
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.postId").value(post.getId().toString()))
            .andExpect(jsonPath("$.authorId").value(AUTHOR_ID.toString()))
            .andExpect(jsonPath("$.content").value(DEFAULT_CONTENT))
            .andExpect(jsonPath("$.status").value(CommentStatus.ACTIVE.toString()));

        assertThat(commentRepository.count()).isEqualTo(countBefore + 1);
    }

    @Test
    @Transactional
    void createCommentRejectsMissingPostId() throws Exception {
        CreateCommentRequestDTO request = new CreateCommentRequestDTO(null, DEFAULT_CONTENT);

        restCommentMockMvc
            .perform(
                post(ENTITY_API_URL + "/create")
                    .with(jwt().jwt(token -> token.subject(AUTHOR_ID.toString())))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(request))
            )
            .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    void createCommentRejectsBlankContent() throws Exception {
        CreateCommentRequestDTO request = new CreateCommentRequestDTO(POST_ID, " ");

        restCommentMockMvc
            .perform(
                post(ENTITY_API_URL + "/create")
                    .with(jwt().jwt(token -> token.subject(AUTHOR_ID.toString())))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(request))
            )
            .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    void getAllCommentsReturnsActiveComments() throws Exception {
        comment = saveCommentForPublicPost(CommentStatus.ACTIVE);

        restCommentMockMvc
            .perform(
                get(ENTITY_API_URL + "?sort=id,desc&postId.equals=" + comment.getPostId())
                    .with(jwt().jwt(token -> token.subject(AUTHOR_ID.toString())))
            )
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(comment.getId().toString())))
            .andExpect(jsonPath("$.[*].content").value(hasItem(DEFAULT_CONTENT)));
    }

    @Test
    @Transactional
    void getAllCommentsHidesDeletedCommentsFromRegularUsers() throws Exception {
        comment = saveCommentForPublicPost(CommentStatus.DELETED);

        restCommentMockMvc
            .perform(
                get(ENTITY_API_URL + "?postId.equals=" + comment.getPostId() + "&id.equals=" + comment.getId())
                    .with(jwt().jwt(token -> token.subject(AUTHOR_ID.toString())))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @Transactional
    void getCommentReturnsCommentById() throws Exception {
        comment = commentRepository.saveAndFlush(comment);

        restCommentMockMvc
            .perform(
                get(ENTITY_API_URL + "/{id}", comment.getId())
                    .with(jwt().jwt(token -> token.subject(AUTHOR_ID.toString())))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(comment.getId().toString()))
            .andExpect(jsonPath("$.postId").value(POST_ID.toString()))
            .andExpect(jsonPath("$.authorId").value(AUTHOR_ID.toString()))
            .andExpect(jsonPath("$.content").value(DEFAULT_CONTENT));
    }

    @Test
    @Transactional
    void countCommentsSupportsCriteria() throws Exception {
        comment = saveCommentForPublicPost(CommentStatus.ACTIVE);

        restCommentMockMvc
            .perform(
                get(ENTITY_API_URL + "/count?postId.equals=" + comment.getPostId())
                    .with(jwt().jwt(token -> token.subject(AUTHOR_ID.toString())))
            )
            .andExpect(status().isOk())
            .andExpect(content().string("1"));
    }

    @Test
    @Transactional
    void getNonExistingCommentReturnsNotFound() throws Exception {
        restCommentMockMvc.perform(get(ENTITY_API_URL + "/{id}", UUID.randomUUID())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void updateOwnCommentChangesOnlyContent() throws Exception {
        comment = commentRepository.saveAndFlush(comment);
        UpdateCommentRequestDTO request = new UpdateCommentRequestDTO(UPDATED_CONTENT);

        restCommentMockMvc
            .perform(
                put(ENTITY_API_URL + "/{id}", comment.getId())
                    .with(jwt().jwt(token -> token.subject(AUTHOR_ID.toString())))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(request))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").value(UPDATED_CONTENT));

        Comment persisted = commentRepository.findById(comment.getId()).orElseThrow();
        assertThat(persisted.getContent()).isEqualTo(UPDATED_CONTENT);
        assertThat(persisted.getAuthorId()).isEqualTo(AUTHOR_ID);
        assertThat(persisted.getPostId()).isEqualTo(POST_ID);
    }

    @Test
    @Transactional
    void updateAnotherUsersCommentIsForbidden() throws Exception {
        comment = commentRepository.saveAndFlush(comment);
        UpdateCommentRequestDTO request = new UpdateCommentRequestDTO(UPDATED_CONTENT);

        restCommentMockMvc
            .perform(
                put(ENTITY_API_URL + "/{id}", comment.getId())
                    .with(jwt().jwt(token -> token.subject(OTHER_USER_ID.toString())))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(request))
            )
            .andExpect(status().isForbidden());

        assertThat(commentRepository.findById(comment.getId()).orElseThrow().getContent()).isEqualTo(DEFAULT_CONTENT);
    }

    @Test
    @Transactional
    void updateNonExistingCommentReturnsNotFound() throws Exception {
        UpdateCommentRequestDTO request = new UpdateCommentRequestDTO(UPDATED_CONTENT);

        restCommentMockMvc
            .perform(
                put(ENTITY_API_URL + "/{id}", UUID.randomUUID())
                    .with(jwt().jwt(token -> token.subject(AUTHOR_ID.toString())))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(request))
            )
            .andExpect(status().isNotFound());
    }

    @Autowired
    private com.minh.fakebook.comment.service.CommentService commentService;

    @Autowired
    private org.springframework.transaction.PlatformTransactionManager transactions;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private com.minh.fakebook.comment.client.UserServiceClient userService;

    private void editComment(UUID id, String text) throws Exception {
        restCommentMockMvc.perform(put(ENTITY_API_URL + "/{id}", id)
            .with(jwt().jwt(token -> token.subject(AUTHOR_ID.toString())))
            .with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsBytes(new UpdateCommentRequestDTO(text))))
            .andExpect(status().isOk());
    }

    @Test
    @Transactional
    void historyPreservesBaselineAndPaginatesActualEdits() throws Exception {
        comment = commentRepository.saveAndFlush(comment);
        editComment(comment.getId(), UPDATED_CONTENT);
        editComment(comment.getId(), UPDATED_CONTENT);
        editComment(comment.getId(), "Third content");
        restCommentMockMvc.perform(get(ENTITY_API_URL + "/{id}", comment.getId()))
            .andExpect(jsonPath("$.editCount").value(2)).andExpect(jsonPath("$.editedAt").isNotEmpty());
        restCommentMockMvc.perform(get(ENTITY_API_URL + "/{id}/history?size=1", comment.getId()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].revision").value(2)).andExpect(jsonPath("$[0].content").value("Third content"));
        restCommentMockMvc.perform(get(ENTITY_API_URL + "/{id}/history?before=2", comment.getId()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].content").value(UPDATED_CONTENT))
            .andExpect(jsonPath("$[1].revision").value(0)).andExpect(jsonPath("$[1].content").value(DEFAULT_CONTENT));
    }

    @Test
    @Transactional
    void newCommentHasNoEditsAndMissingHistoryReturns404() throws Exception {
        comment = commentRepository.saveAndFlush(comment);
        restCommentMockMvc.perform(get(ENTITY_API_URL + "/{id}", comment.getId()))
            .andExpect(jsonPath("$.editCount").value(0)).andExpect(jsonPath("$.editedAt").isEmpty());
        restCommentMockMvc.perform(get(ENTITY_API_URL + "/{id}/history", comment.getId()))
            .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
        restCommentMockMvc.perform(get(ENTITY_API_URL + "/{id}/history", UUID.randomUUID()))
            .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void historyRechecksCurrentVisibilityAndDeletedPolicy() throws Exception {
        comment = saveCommentForPublicPost(CommentStatus.ACTIVE);
        editComment(comment.getId(), UPDATED_CONTENT);
        UUID id = comment.getId();
        restCommentMockMvc.perform(get(ENTITY_API_URL + "/{id}/history", id)
            .with(jwt().jwt(token -> token.subject(OTHER_USER_ID.toString())))).andExpect(status().isOk());
        var post = postCacheRepository.findById(comment.getPostId()).orElseThrow();
        post.setVisibility("PRIVATE"); postCacheRepository.saveAndFlush(post);
        restCommentMockMvc.perform(get(ENTITY_API_URL + "/{id}/history", id)
            .with(jwt().jwt(token -> token.subject(OTHER_USER_ID.toString())))).andExpect(status().isForbidden());
        post.setVisibility("FRIENDS"); postCacheRepository.saveAndFlush(post);
        org.mockito.Mockito.when(userService.checkFriendship(OTHER_USER_ID, post.getAuthorId())).thenReturn(false);
        restCommentMockMvc.perform(get(ENTITY_API_URL + "/{id}/history", id)
            .with(jwt().jwt(token -> token.subject(OTHER_USER_ID.toString())))).andExpect(status().isForbidden());
        org.mockito.Mockito.when(userService.checkFriendship(OTHER_USER_ID, post.getAuthorId())).thenReturn(true);
        restCommentMockMvc.perform(get(ENTITY_API_URL + "/{id}/history", id)
            .with(jwt().jwt(token -> token.subject(OTHER_USER_ID.toString())))).andExpect(status().isOk());
        post.setVisibility("PUBLIC"); postCacheRepository.saveAndFlush(post);
        comment.setStatus(CommentStatus.DELETED); commentRepository.saveAndFlush(comment);
        restCommentMockMvc.perform(get(ENTITY_API_URL + "/{id}/history", id)
            .with(jwt().jwt(token -> token.subject(OTHER_USER_ID.toString())))).andExpect(status().isForbidden());
        restCommentMockMvc.perform(get(ENTITY_API_URL + "/{id}/history", id)
            .with(jwt().jwt(token -> token.subject(AUTHOR_ID.toString())))).andExpect(status().isOk());
        restCommentMockMvc.perform(put(ENTITY_API_URL + "/{id}", id)
            .with(jwt().jwt(token -> token.subject(AUTHOR_ID.toString())))
            .with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsBytes(new UpdateCommentRequestDTO("Cannot edit"))))
            .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    void replyAndAdminEditKeepEditorAndPreviewMetadata() throws Exception {
        var parent = commentRepository.saveAndFlush(comment);
        var reply = commentRepository.saveAndFlush(new Comment().postId(POST_ID).authorId(AUTHOR_ID)
            .parentComment(parent).content("Reply").status(CommentStatus.ACTIVE));
        editComment(reply.getId(), "Edited reply");
        restCommentMockMvc.perform(put(ENTITY_API_URL + "/{id}", parent.getId())
            .with(jwt().jwt(token -> token.subject(OTHER_USER_ID.toString()))
                .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority(AuthoritiesConstants.ADMIN)))
            .with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsBytes(new UpdateCommentRequestDTO(UPDATED_CONTENT))))
            .andExpect(status().isOk());
        restCommentMockMvc.perform(get(ENTITY_API_URL + "/{id}/history", parent.getId()))
            .andExpect(jsonPath("$[0].editorId").value(OTHER_USER_ID.toString()));
        restCommentMockMvc.perform(post(ENTITY_API_URL + "/summaries").with(csrf())
            .contentType(MediaType.APPLICATION_JSON).content("{\"postIds\":[\"" + POST_ID + "\"]}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0].previewComment.editCount").value(1))
            .andExpect(jsonPath("$[0].previewComment.editedAt").isNotEmpty());
    }

    @Test
    void concurrentEditsAndRollbackPreserveHistory() throws Exception {
        var tx = new org.springframework.transaction.support.TransactionTemplate(transactions);
        UUID id = tx.execute(t -> commentRepository.saveAndFlush(comment).getId());
        var pool = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            var start = new java.util.concurrent.CountDownLatch(1);
            java.util.List<java.util.concurrent.Future<?>> edits = new java.util.ArrayList<>();
            for (String text : java.util.List.of("Concurrent A", "Concurrent B")) {
                edits.add(pool.submit(() -> { start.await(); commentService.updateComment(id, text, AUTHOR_ID); return null; }));
            }
            start.countDown();
            for (var edit : edits) edit.get(30, java.util.concurrent.TimeUnit.SECONDS);
            assertThat(commentRepository.findById(id).orElseThrow().getEditCount()).isEqualTo(2);
            assertThat(fixtureJdbc.queryForObject("SELECT COUNT(*) FROM comment_revision WHERE comment_id = ?", Long.class, id.toString())).isEqualTo(3);
            org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () -> tx.execute(t -> {
                commentService.updateComment(id, "Rolled back", AUTHOR_ID);
                throw new IllegalStateException("Abort transaction");
            }));
            assertThat(commentRepository.findById(id).orElseThrow().getEditCount()).isEqualTo(2);
            assertThat(fixtureJdbc.queryForObject("SELECT COUNT(*) FROM comment_revision WHERE comment_id = ?", Long.class, id.toString())).isEqualTo(3);
        } finally {
            pool.shutdownNow();
            tx.executeWithoutResult(t -> commentRepository.deleteById(id));
        }
    }

    @Test
    @Transactional
    void deleteOwnCommentSoftDeletesIt() throws Exception {
        comment = commentRepository.saveAndFlush(comment);

        restCommentMockMvc
            .perform(
                delete(ENTITY_API_URL + "/{id}", comment.getId())
                    .with(jwt().jwt(token -> token.subject(AUTHOR_ID.toString())))
                    .with(csrf())
            )
            .andExpect(status().isNoContent());

        assertThat(commentRepository.findById(comment.getId()).orElseThrow().getStatus()).isEqualTo(CommentStatus.DELETED);
    }

    @Test
    @Transactional
    void deleteAnotherUsersCommentIsForbidden() throws Exception {
        comment = commentRepository.saveAndFlush(comment);

        restCommentMockMvc
            .perform(
                delete(ENTITY_API_URL + "/{id}", comment.getId())
                    .with(jwt().jwt(token -> token.subject(OTHER_USER_ID.toString())))
                    .with(csrf())
            )
            .andExpect(status().isForbidden());

        assertThat(commentRepository.findById(comment.getId()).orElseThrow().getStatus()).isEqualTo(CommentStatus.ACTIVE);
    }

    @Test
    @Transactional
    void replyToCommentCreatesActiveChildComment() throws Exception {
        comment = commentRepository.saveAndFlush(comment);
        ReplyCommentRequestDTO request = new ReplyCommentRequestDTO(comment.getId(), "A reply");
        long countBefore = commentRepository.count();

        restCommentMockMvc
            .perform(
                post(ENTITY_API_URL + "/reply")
                    .with(jwt().jwt(token -> token.subject(OTHER_USER_ID.toString())))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(request))
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.postId").value(POST_ID.toString()))
            .andExpect(jsonPath("$.authorId").value(OTHER_USER_ID.toString()))
            .andExpect(jsonPath("$.content").value("A reply"))
            .andExpect(jsonPath("$.status").value(CommentStatus.ACTIVE.toString()))
            .andExpect(jsonPath("$.parentComment.id").value(comment.getId().toString()));

        assertThat(commentRepository.count()).isEqualTo(countBefore + 1);
    }

    @Test
    @Transactional
    void replyToDeletedCommentIsRejected() throws Exception {
        comment.status(CommentStatus.DELETED);
        comment = commentRepository.saveAndFlush(comment);
        ReplyCommentRequestDTO request = new ReplyCommentRequestDTO(comment.getId(), "A reply");

        restCommentMockMvc
            .perform(
                post(ENTITY_API_URL + "/reply")
                    .with(jwt().jwt(token -> token.subject(OTHER_USER_ID.toString())))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(request))
            )
            .andExpect(status().isBadRequest());
    }

    private Comment saveCommentForPublicPost(CommentStatus status) {
        PostCache post = postCacheRepository.saveAndFlush(
            new PostCache().authorId(AUTHOR_ID).visibility("PUBLIC").status("ACTIVE")
        );
        return commentRepository.saveAndFlush(comment.postId(post.getId()).status(status));
    }
}
