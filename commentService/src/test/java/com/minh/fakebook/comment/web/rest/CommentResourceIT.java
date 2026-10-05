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
    private MockMvc restCommentMockMvc;

    private Comment comment;

    @BeforeEach
    void initTest() {
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
            .perform(get(ENTITY_API_URL + "/{id}", comment.getId()))
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
