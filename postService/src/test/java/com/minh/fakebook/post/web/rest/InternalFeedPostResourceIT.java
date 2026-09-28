package com.minh.fakebook.post.web.rest;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.minh.fakebook.post.IntegrationTest;
import com.minh.fakebook.post.domain.Post;
import com.minh.fakebook.post.domain.enumeration.PostStatus;
import com.minh.fakebook.post.domain.enumeration.PostVisibility;
import com.minh.fakebook.post.repository.PostRepository;
import com.minh.fakebook.post.security.AuthoritiesConstants;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@AutoConfigureMockMvc
class InternalFeedPostResourceIT {

    private static final String API_URL = "/api/internal/feed-posts";

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @Transactional
    void returnsOnlyRecentActiveFriendsPostsForRequestedAuthor() throws Exception {
        UUID authorId = UUID.randomUUID();
        UUID otherAuthorId = UUID.randomUUID();
        Instant now = Instant.now();

        Post older = savePost(authorId, PostVisibility.FRIENDS, PostStatus.ACTIVE, now.minusSeconds(20));
        Post newer = savePost(authorId, PostVisibility.FRIENDS, PostStatus.ACTIVE, now.minusSeconds(10));
        savePost(authorId, PostVisibility.PUBLIC, PostStatus.ACTIVE, now);
        savePost(authorId, PostVisibility.PRIVATE, PostStatus.ACTIVE, now);
        savePost(authorId, PostVisibility.FRIENDS, PostStatus.DELETED, now);
        savePost(otherAuthorId, PostVisibility.FRIENDS, PostStatus.ACTIVE, now);

        mockMvc
            .perform(
                get(API_URL)
                    .param("authorId", authorId.toString())
                    .param("limit", "10")
                    .with(jwt().authorities(new SimpleGrantedAuthority(AuthoritiesConstants.INTERNAL)))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].postId").value(newer.getId().toString()))
            .andExpect(jsonPath("$[0].authorId").value(authorId.toString()))
            .andExpect(jsonPath("$[0].visibility").value(PostVisibility.FRIENDS.toString()))
            .andExpect(jsonPath("$[1].postId").value(older.getId().toString()));
    }

    @Test
    @Transactional
    void honorsRequestedLimit() throws Exception {
        UUID authorId = UUID.randomUUID();
        Instant now = Instant.now();
        savePost(authorId, PostVisibility.FRIENDS, PostStatus.ACTIVE, now.minusSeconds(1));
        savePost(authorId, PostVisibility.FRIENDS, PostStatus.ACTIVE, now);

        mockMvc
            .perform(
                get(API_URL)
                    .param("authorId", authorId.toString())
                    .param("limit", "1")
                    .with(jwt().authorities(new SimpleGrantedAuthority(AuthoritiesConstants.INTERNAL)))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void rejectsRegularUser() throws Exception {
        mockMvc
            .perform(
                get(API_URL)
                    .param("authorId", UUID.randomUUID().toString())
                    .with(jwt().authorities(new SimpleGrantedAuthority(AuthoritiesConstants.USER)))
            )
            .andExpect(status().isForbidden());
    }

    @Test
    void rejectsAnonymousRequest() throws Exception {
        mockMvc.perform(get(API_URL).param("authorId", UUID.randomUUID().toString())).andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsLimitOutsideSupportedRange() throws Exception {
        mockMvc
            .perform(
                get(API_URL)
                    .param("authorId", UUID.randomUUID().toString())
                    .param("limit", "0")
                    .with(jwt().authorities(new SimpleGrantedAuthority(AuthoritiesConstants.INTERNAL)))
            )
            .andExpect(status().isBadRequest());

        mockMvc
            .perform(
                get(API_URL)
                    .param("authorId", UUID.randomUUID().toString())
                    .param("limit", "501")
                    .with(jwt().authorities(new SimpleGrantedAuthority(AuthoritiesConstants.INTERNAL)))
            )
            .andExpect(status().isBadRequest());
    }

    private Post savePost(UUID authorId, PostVisibility visibility, PostStatus status, Instant createdAt) {
        Post post = new Post()
            .authorId(authorId)
            .content("feed backfill")
            .visibility(visibility)
            .status(status)
            .createdAt(createdAt);
        return postRepository.saveAndFlush(post);
    }
}
