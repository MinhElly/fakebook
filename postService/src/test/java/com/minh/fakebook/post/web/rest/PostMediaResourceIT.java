package com.minh.fakebook.post.web.rest;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.minh.fakebook.post.IntegrationTest;
import com.minh.fakebook.post.domain.Post;
import com.minh.fakebook.post.domain.PostMedia;
import com.minh.fakebook.post.domain.enumeration.PostStatus;
import com.minh.fakebook.post.domain.enumeration.PostVisibility;
import com.minh.fakebook.post.repository.PostMediaRepository;
import com.minh.fakebook.post.repository.PostRepository;
import com.minh.fakebook.post.service.dto.PostMediaDTO;
import com.minh.fakebook.post.service.mapper.PostMediaMapper;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/** Integration tests for the supported read-only {@link PostMediaResource} contract. */
@IntegrationTest
@AutoConfigureMockMvc
class PostMediaResourceIT {

    private static final UUID AUTHOR_ID = UUID.randomUUID();
    private static final UUID MEDIA_ID = UUID.randomUUID();
    private static final String ENTITY_API_URL = "/api/post-medias";

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostMediaRepository postMediaRepository;

    @Autowired
    private PostMediaMapper postMediaMapper;

    @Autowired
    private MockMvc restPostMediaMockMvc;

    private Post post;
    private PostMedia postMedia;

    @BeforeEach
    void initTest() {
        post = new Post()
            .authorId(AUTHOR_ID)
            .content("A post")
            .visibility(PostVisibility.PUBLIC)
            .status(PostStatus.ACTIVE)
            .createdAt(Instant.now());
        postMedia = new PostMedia().mediaId(MEDIA_ID).displayOrder(0).createdAt(Instant.now()).post(post);
    }

    @Test
    @Transactional
    void authenticatedUserCanGetAllPostMedia() throws Exception {
        post = postRepository.saveAndFlush(post);
        postMedia.post(post);
        postMedia = postMediaRepository.saveAndFlush(postMedia);

        restPostMediaMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc").with(jwt()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(postMedia.getId().toString())))
            .andExpect(jsonPath("$.[*].mediaId").value(hasItem(MEDIA_ID.toString())));
    }

    @Test
    @Transactional
    void authenticatedUserCanFilterAndCountPostMedia() throws Exception {
        post = postRepository.saveAndFlush(post);
        postMedia.post(post);
        postMedia = postMediaRepository.saveAndFlush(postMedia);

        restPostMediaMockMvc
            .perform(get(ENTITY_API_URL + "?mediaId.equals=" + MEDIA_ID).with(jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.[*].id").value(hasItem(postMedia.getId().toString())));

        restPostMediaMockMvc
            .perform(get(ENTITY_API_URL + "/count?mediaId.equals=" + MEDIA_ID).with(jwt()))
            .andExpect(status().isOk())
            .andExpect(content().string("1"));
    }

    @Test
    @Transactional
    void authenticatedUserCanGetPostMediaById() throws Exception {
        post = postRepository.saveAndFlush(post);
        postMedia.post(post);
        postMedia = postMediaRepository.saveAndFlush(postMedia);

        restPostMediaMockMvc
            .perform(get(ENTITY_API_URL + "/{id}", postMedia.getId()).with(jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(postMedia.getId().toString()))
            .andExpect(jsonPath("$.mediaId").value(MEDIA_ID.toString()))
            .andExpect(jsonPath("$.post.id").value(post.getId().toString()));
    }

    @Test
    @Transactional
    void getPostMediaRequiresAuthentication() throws Exception {
        restPostMediaMockMvc.perform(get(ENTITY_API_URL)).andExpect(status().isUnauthorized());
    }

    @Test
    @Transactional
    void getNonExistingPostMediaReturnsNotFound() throws Exception {
        restPostMediaMockMvc
            .perform(get(ENTITY_API_URL + "/{id}", UUID.randomUUID()).with(jwt()))
            .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void directCreateWithExistingIdIsRejectedBeforeServiceInvocation() throws Exception {
        post = postRepository.saveAndFlush(post);
        postMedia.post(post);
        postMedia = postMediaRepository.saveAndFlush(postMedia);
        PostMediaDTO request = postMediaMapper.toDto(postMedia);

        restPostMediaMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(jwt())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(request))
            )
            .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    void directCreateValidatesRequiredFields() throws Exception {
        post = postRepository.saveAndFlush(post);
        postMedia.post(post).mediaId(null);
        PostMediaDTO request = postMediaMapper.toDto(postMedia);

        restPostMediaMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(jwt())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(request))
            )
            .andExpect(status().isBadRequest());
    }
}
