package com.minh.fakebook.post.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.minh.fakebook.post.IntegrationTest;
import com.minh.fakebook.post.domain.Post;
import com.minh.fakebook.post.domain.enumeration.PostStatus;
import com.minh.fakebook.post.domain.enumeration.PostVisibility;
import com.minh.fakebook.post.repository.PostRepository;
import com.minh.fakebook.post.security.AuthoritiesConstants;
import com.minh.fakebook.post.service.dto.CreatePostRequestDTO;
import com.minh.fakebook.post.service.dto.PostDTO;
import com.minh.fakebook.post.service.mapper.PostMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/** Integration tests for the supported {@link PostResource} API contract. */
@IntegrationTest
@AutoConfigureMockMvc
class PostResourceIT {

    private static final UUID AUTHOR_ID = UUID.randomUUID();
    private static final UUID OTHER_USER_ID = UUID.randomUUID();
    private static final String DEFAULT_CONTENT = "A post";
    private static final String UPDATED_CONTENT = "An updated post";
    private static final String ENTITY_API_URL = "/api/posts";

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private MockMvc restPostMockMvc;

    private Post post;

    @BeforeEach
    void initTest() {
        post = newPost(AUTHOR_ID, PostVisibility.PUBLIC);
    }

    @Test
    @Transactional
    void createPostUsesAuthenticatedUserAsAuthor() throws Exception {
        long countBefore = postRepository.count();
        CreatePostRequestDTO request = new CreatePostRequestDTO(DEFAULT_CONTENT, PostVisibility.PUBLIC, List.of(), List.of());

        restPostMockMvc
            .perform(
                post(ENTITY_API_URL + "/create")
                    .with(jwtAs(AUTHOR_ID))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(request))
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.authorId").value(AUTHOR_ID.toString()))
            .andExpect(jsonPath("$.content").value(DEFAULT_CONTENT))
            .andExpect(jsonPath("$.visibility").value(PostVisibility.PUBLIC.toString()))
            .andExpect(jsonPath("$.status").value(PostStatus.ACTIVE.toString()));

        assertThat(postRepository.count()).isEqualTo(countBefore + 1);
    }

    @Test
    @Transactional
    void createPostRequiresAuthentication() throws Exception {
        CreatePostRequestDTO request = new CreatePostRequestDTO(DEFAULT_CONTENT, PostVisibility.PUBLIC, List.of(), List.of());

        restPostMockMvc
            .perform(
                post(ENTITY_API_URL + "/create")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(request))
            )
            .andExpect(status().isUnauthorized());
    }

    @Test
    @Transactional
    void guestFeedReturnsActivePublicPostsOnly() throws Exception {
        Post publicPost = postRepository.saveAndFlush(post);
        Post privatePost = postRepository.saveAndFlush(newPost(OTHER_USER_ID, PostVisibility.PRIVATE));
        Post deletedPost = postRepository.saveAndFlush(newPost(OTHER_USER_ID, PostVisibility.PUBLIC).status(PostStatus.DELETED));

        restPostMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(publicPost.getId().toString())))
            .andExpect(jsonPath("$.[?(@.id == '%s')]", privatePost.getId()).isEmpty())
            .andExpect(jsonPath("$.[?(@.id == '%s')]", deletedPost.getId()).isEmpty());
    }

    @Test
    @Transactional
    void getPublicPostReturnsPost() throws Exception {
        post = postRepository.saveAndFlush(post);

        restPostMockMvc
            .perform(get(ENTITY_API_URL + "/{id}", post.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(post.getId().toString()))
            .andExpect(jsonPath("$.authorId").value(AUTHOR_ID.toString()))
            .andExpect(jsonPath("$.content").value(DEFAULT_CONTENT));
    }

    @Test
    @Transactional
    void privatePostIsVisibleToItsAuthor() throws Exception {
        post.visibility(PostVisibility.PRIVATE);
        post = postRepository.saveAndFlush(post);

        restPostMockMvc
            .perform(get(ENTITY_API_URL + "/{id}", post.getId()).with(jwtAs(AUTHOR_ID)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(post.getId().toString()));
    }

    @Test
    @Transactional
    void privatePostIsForbiddenToAnotherUser() throws Exception {
        post.visibility(PostVisibility.PRIVATE);
        post = postRepository.saveAndFlush(post);

        restPostMockMvc
            .perform(get(ENTITY_API_URL + "/{id}", post.getId()).with(jwtAs(OTHER_USER_ID)))
            .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    void getNonExistingPostReturnsNotFound() throws Exception {
        restPostMockMvc.perform(get(ENTITY_API_URL + "/{id}", UUID.randomUUID())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void countPostsAppliesVisibilityAndCriteria() throws Exception {
        postRepository.saveAndFlush(post);
        postRepository.saveAndFlush(newPost(OTHER_USER_ID, PostVisibility.PRIVATE));

        restPostMockMvc
            .perform(get(ENTITY_API_URL + "/count?authorId.equals=" + AUTHOR_ID))
            .andExpect(status().isOk())
            .andExpect(content().string("1"));
    }

    @Test
    @Transactional
    void updateOwnPostChangesAllowedFields() throws Exception {
        post = postRepository.saveAndFlush(post);
        PostDTO request = postMapper.toDto(post);
        request.setContent(UPDATED_CONTENT);
        request.setVisibility(PostVisibility.FRIENDS);
        request.setMediaIds(List.of());

        restPostMockMvc
            .perform(
                put(ENTITY_API_URL + "/{id}", post.getId())
                    .with(jwtAs(AUTHOR_ID))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(request))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").value(UPDATED_CONTENT))
            .andExpect(jsonPath("$.visibility").value(PostVisibility.FRIENDS.toString()));

        Post persisted = postRepository.findById(post.getId()).orElseThrow();
        assertThat(persisted.getAuthorId()).isEqualTo(AUTHOR_ID);
        assertThat(persisted.getStatus()).isEqualTo(PostStatus.ACTIVE);
        assertThat(persisted.getContent()).isEqualTo(UPDATED_CONTENT);
    }

    @Test
    @Transactional
    void updateAnotherUsersPostIsForbidden() throws Exception {
        post = postRepository.saveAndFlush(post);
        PostDTO request = postMapper.toDto(post);
        request.setContent(UPDATED_CONTENT);
        request.setMediaIds(List.of());

        restPostMockMvc
            .perform(
                put(ENTITY_API_URL + "/{id}", post.getId())
                    .with(jwtAs(OTHER_USER_ID))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(request))
            )
            .andExpect(status().isForbidden());

        assertThat(postRepository.findById(post.getId()).orElseThrow().getContent()).isEqualTo(DEFAULT_CONTENT);
    }

    @Test
    @Transactional
    void patchOwnPostDoesNotMassAssignAuthorOrStatus() throws Exception {
        post = postRepository.saveAndFlush(post);
        PostDTO request = new PostDTO();
        request.setId(post.getId());
        request.setAuthorId(OTHER_USER_ID);
        request.setContent(UPDATED_CONTENT);
        request.setStatus(PostStatus.DELETED);

        restPostMockMvc
            .perform(
                patch(ENTITY_API_URL + "/{id}", post.getId())
                    .with(jwtAs(AUTHOR_ID))
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(objectMapper.writeValueAsBytes(request))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").value(UPDATED_CONTENT));

        Post persisted = postRepository.findById(post.getId()).orElseThrow();
        assertThat(persisted.getAuthorId()).isEqualTo(AUTHOR_ID);
        assertThat(persisted.getStatus()).isEqualTo(PostStatus.ACTIVE);
    }

    @Test
    @Transactional
    void deleteOwnPostRemovesIt() throws Exception {
        post = postRepository.saveAndFlush(post);

        restPostMockMvc
            .perform(delete(ENTITY_API_URL + "/{id}", post.getId()).with(jwtAs(AUTHOR_ID)).with(csrf()))
            .andExpect(status().isNoContent());

        assertThat(postRepository.findById(post.getId())).isEmpty();
    }

    @Test
    @Transactional
    void deleteAnotherUsersPostIsForbidden() throws Exception {
        post = postRepository.saveAndFlush(post);

        restPostMockMvc
            .perform(delete(ENTITY_API_URL + "/{id}", post.getId()).with(jwtAs(OTHER_USER_ID)).with(csrf()))
            .andExpect(status().isForbidden());

        assertThat(postRepository.findById(post.getId())).isPresent();
    }

    public static Post createEntity() {
        return newPost(AUTHOR_ID, PostVisibility.PUBLIC);
    }

    public static Post createUpdatedEntity() {
        return newPost(AUTHOR_ID, PostVisibility.FRIENDS).content(UPDATED_CONTENT).updatedAt(Instant.now());
    }

    private static Post newPost(UUID authorId, PostVisibility visibility) {
        return new Post()
            .authorId(authorId)
            .content(DEFAULT_CONTENT)
            .visibility(visibility)
            .status(PostStatus.ACTIVE)
            .createdAt(Instant.now());
    }

    private static RequestPostProcessor jwtAs(UUID userId) {
        return jwt()
            .jwt(token -> token.subject(userId.toString()))
            .authorities(new SimpleGrantedAuthority(AuthoritiesConstants.USER));
    }
}
