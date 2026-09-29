package com.minh.fakebook.comment.web.rest;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.minh.fakebook.comment.IntegrationTest;
import com.minh.fakebook.comment.domain.Comment;
import com.minh.fakebook.comment.domain.PostCache;
import com.minh.fakebook.comment.domain.enumeration.CommentStatus;
import com.minh.fakebook.comment.repository.CommentRepository;
import com.minh.fakebook.comment.repository.PostCacheRepository;
import com.minh.fakebook.comment.security.AuthoritiesConstants;
import com.minh.fakebook.comment.service.dto.CommentSummaryRequest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser(authorities = AuthoritiesConstants.USER)
class CommentSummaryResourceIT {

    private static final String ENTITY_API_URL = "/api/comments/summaries";

    private static final UUID CURRENT_USER_ID = UUID.randomUUID();

    private static final UUID OTHER_USER_ID = UUID.randomUUID();

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private PostCacheRepository postCacheRepository;

    @Autowired
    private MockMvc restCommentMockMvc;

    @Test
    @Transactional
    void getSummariesReturnsCountsAndLatestRootPreviewInInputOrder() throws Exception {
        PostCache firstPost = savePost(CURRENT_USER_ID, "PUBLIC");
        PostCache emptyPost = savePost(OTHER_USER_ID, "PUBLIC");
        Instant olderTime = Instant.parse("2026-09-28T08:00:00Z");
        Instant newerTime = Instant.parse("2026-09-28T09:00:00Z");

        Comment olderRoot = saveComment(firstPost.getId(), "older root", CommentStatus.ACTIVE, null, olderTime);
        Comment newerRoot = saveComment(firstPost.getId(), "newer root", CommentStatus.ACTIVE, null, newerTime);
        saveComment(firstPost.getId(), "reply", CommentStatus.ACTIVE, olderRoot, newerTime.plusSeconds(60));
        saveComment(firstPost.getId(), "deleted", CommentStatus.DELETED, null, newerTime.plusSeconds(120));

        CommentSummaryRequest request = new CommentSummaryRequest(
            List.of(firstPost.getId(), emptyPost.getId(), firstPost.getId())
        );

        restCommentMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(jwt().jwt(token -> token.subject(CURRENT_USER_ID.toString())))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(request))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].postId").value(firstPost.getId().toString()))
            .andExpect(jsonPath("$[0].commentCount").value(3))
            .andExpect(jsonPath("$[0].previewComment.id").value(newerRoot.getId().toString()))
            .andExpect(jsonPath("$[0].previewComment.content").value("newer root"))
            .andExpect(jsonPath("$[1].postId").value(emptyPost.getId().toString()))
            .andExpect(jsonPath("$[1].commentCount").value(0))
            .andExpect(jsonPath("$[1].previewComment").doesNotExist());
    }

    @Test
    @Transactional
    void getSummariesRejectsPrivatePostForNonOwner() throws Exception {
        PostCache privatePost = savePost(OTHER_USER_ID, "PRIVATE");
        CommentSummaryRequest request = new CommentSummaryRequest(List.of(privatePost.getId()));

        restCommentMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(jwt().jwt(token -> token.subject(CURRENT_USER_ID.toString())))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(request))
            )
            .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    void getSummariesRejectsEmptyAndOversizedRequests() throws Exception {
        CommentSummaryRequest emptyRequest = new CommentSummaryRequest(List.of());
        List<UUID> tooManyPostIds = new ArrayList<>();
        for (int index = 0; index < 51; index++) {
            tooManyPostIds.add(UUID.randomUUID());
        }

        restCommentMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(jwt().jwt(token -> token.subject(CURRENT_USER_ID.toString())))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(emptyRequest))
            )
            .andExpect(status().isBadRequest());

        restCommentMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(jwt().jwt(token -> token.subject(CURRENT_USER_ID.toString())))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(new CommentSummaryRequest(tooManyPostIds)))
            )
            .andExpect(status().isBadRequest());
    }

    private PostCache savePost(UUID authorId, String visibility) {
        return postCacheRepository.saveAndFlush(
            new PostCache().authorId(authorId).visibility(visibility).status("ACTIVE")
        );
    }

    private Comment saveComment(
        UUID postId,
        String content,
        CommentStatus status,
        Comment parent,
        Instant createdAt
    ) {
        Comment comment = new Comment()
            .postId(postId)
            .authorId(CURRENT_USER_ID)
            .content(content)
            .status(status)
            .parentComment(parent);
        comment.setCreatedAt(createdAt);
        return commentRepository.saveAndFlush(comment);
    }
}
