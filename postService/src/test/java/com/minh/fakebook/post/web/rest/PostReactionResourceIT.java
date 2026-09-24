package com.minh.fakebook.post.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.minh.fakebook.post.IntegrationTest;
import com.minh.fakebook.post.domain.Post;
import com.minh.fakebook.post.domain.PostReaction;
import com.minh.fakebook.post.domain.enumeration.PostStatus;
import com.minh.fakebook.post.domain.enumeration.PostVisibility;
import com.minh.fakebook.post.domain.enumeration.ReactionType;
import com.minh.fakebook.post.repository.PostReactionRepository;
import com.minh.fakebook.post.repository.PostRepository;
import com.minh.fakebook.post.security.AuthoritiesConstants;
import com.minh.fakebook.post.service.dto.PostDTO;
import com.minh.fakebook.post.service.dto.PostReactionDTO;
import java.time.Instant;
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

/** Integration tests for the supported {@link PostReactionResource} API contract. */
@IntegrationTest
@AutoConfigureMockMvc
class PostReactionResourceIT {

    private static final UUID AUTHOR_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID OTHER_USER_ID = UUID.randomUUID();
    private static final String ENTITY_API_URL = "/api/post-reactions";

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostReactionRepository postReactionRepository;

    @Autowired
    private MockMvc restPostReactionMockMvc;

    private Post post;

    @BeforeEach
    void initTest() {
        post = new Post()
            .authorId(AUTHOR_ID)
            .content("A post")
            .visibility(PostVisibility.PUBLIC)
            .status(PostStatus.ACTIVE)
            .createdAt(Instant.now());
    }

    @Test
    @Transactional
    void guestCanReadReactionsForPublicPost() throws Exception {
        post = postRepository.saveAndFlush(post);
        PostReaction reaction = saveReaction(USER_ID, ReactionType.LIKE);

        restPostReactionMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(reaction.getId().toString())))
            .andExpect(jsonPath("$.[*].userId").value(hasItem(USER_ID.toString())))
            .andExpect(jsonPath("$.[*].reactionType").value(hasItem(ReactionType.LIKE.toString())));
    }

    @Test
    @Transactional
    void guestCannotSeeReactionsForPrivatePost() throws Exception {
        post.visibility(PostVisibility.PRIVATE);
        post = postRepository.saveAndFlush(post);
        PostReaction reaction = saveReaction(USER_ID, ReactionType.LIKE);

        restPostReactionMockMvc
            .perform(get(ENTITY_API_URL + "?id.equals=" + reaction.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @Transactional
    void guestCanFilterAndCountVisibleReactions() throws Exception {
        post = postRepository.saveAndFlush(post);
        PostReaction reaction = saveReaction(USER_ID, ReactionType.LOVE);

        restPostReactionMockMvc
            .perform(get(ENTITY_API_URL + "?userId.equals=" + USER_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.[*].id").value(hasItem(reaction.getId().toString())));

        restPostReactionMockMvc
            .perform(get(ENTITY_API_URL + "/count?userId.equals=" + USER_ID))
            .andExpect(status().isOk())
            .andExpect(content().string("1"));
    }

    @Test
    @Transactional
    void guestCanGetPublicPostReactionById() throws Exception {
        post = postRepository.saveAndFlush(post);
        PostReaction reaction = saveReaction(USER_ID, ReactionType.LIKE);

        restPostReactionMockMvc
            .perform(get(ENTITY_API_URL + "/{id}", reaction.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(reaction.getId().toString()))
            .andExpect(jsonPath("$.post.id").value(post.getId().toString()));
    }

    @Test
    @Transactional
    void getNonExistingPostReactionReturnsNotFound() throws Exception {
        restPostReactionMockMvc.perform(get(ENTITY_API_URL + "/{id}", UUID.randomUUID())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void createReactionUsesAuthenticatedUserId() throws Exception {
        post = postRepository.saveAndFlush(post);
        PostReactionDTO request = reactionRequest(OTHER_USER_ID, ReactionType.LIKE);

        restPostReactionMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(jwtAs(USER_ID))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(request))
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.userId").value(USER_ID.toString()))
            .andExpect(jsonPath("$.reactionType").value(ReactionType.LIKE.toString()));

        assertThat(postReactionRepository.findByPostIdAndUserId(post.getId(), USER_ID)).isPresent();
        assertThat(postReactionRepository.findByPostIdAndUserId(post.getId(), OTHER_USER_ID)).isEmpty();
    }

    @Test
    @Transactional
    void sameReactionTypeTogglesReactionOff() throws Exception {
        post = postRepository.saveAndFlush(post);
        saveReaction(USER_ID, ReactionType.LIKE);

        restPostReactionMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(jwtAs(USER_ID))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(reactionRequest(USER_ID, ReactionType.LIKE)))
            )
            .andExpect(status().isNoContent());

        assertThat(postReactionRepository.findByPostIdAndUserId(post.getId(), USER_ID)).isEmpty();
    }

    @Test
    @Transactional
    void differentReactionTypeUpdatesExistingReaction() throws Exception {
        post = postRepository.saveAndFlush(post);
        saveReaction(USER_ID, ReactionType.LIKE);

        restPostReactionMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(jwtAs(USER_ID))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(reactionRequest(USER_ID, ReactionType.LOVE)))
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.reactionType").value(ReactionType.LOVE.toString()));

        PostReaction updated = postReactionRepository.findByPostIdAndUserId(post.getId(), USER_ID).orElseThrow();
        assertThat(updated.getReactionType()).isEqualTo(ReactionType.LOVE);
        assertThat(updated.getUpdatedAt()).isNotNull();
    }

    @Test
    @Transactional
    void reactionToAnotherUsersPrivatePostIsForbidden() throws Exception {
        post.visibility(PostVisibility.PRIVATE);
        post = postRepository.saveAndFlush(post);

        restPostReactionMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(jwtAs(USER_ID))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(reactionRequest(USER_ID, ReactionType.LIKE)))
            )
            .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    void deleteOwnReactionRemovesIt() throws Exception {
        post = postRepository.saveAndFlush(post);
        PostReaction reaction = saveReaction(USER_ID, ReactionType.LIKE);

        restPostReactionMockMvc
            .perform(delete(ENTITY_API_URL + "/{id}", reaction.getId()).with(jwtAs(USER_ID)).with(csrf()))
            .andExpect(status().isNoContent());

        assertThat(postReactionRepository.findById(reaction.getId())).isEmpty();
    }

    @Test
    @Transactional
    void deleteAnotherUsersReactionIsForbidden() throws Exception {
        post = postRepository.saveAndFlush(post);
        PostReaction reaction = saveReaction(USER_ID, ReactionType.LIKE);

        restPostReactionMockMvc
            .perform(delete(ENTITY_API_URL + "/{id}", reaction.getId()).with(jwtAs(OTHER_USER_ID)).with(csrf()))
            .andExpect(status().isForbidden());

        assertThat(postReactionRepository.findById(reaction.getId())).isPresent();
    }

    private PostReaction saveReaction(UUID userId, ReactionType reactionType) {
        return postReactionRepository.saveAndFlush(
            new PostReaction().userId(userId).reactionType(reactionType).createdAt(Instant.now()).post(post)
        );
    }

    private PostReactionDTO reactionRequest(UUID claimedUserId, ReactionType reactionType) {
        PostDTO postDTO = new PostDTO();
        postDTO.setId(post.getId());

        PostReactionDTO request = new PostReactionDTO();
        request.setUserId(claimedUserId);
        request.setReactionType(reactionType);
        request.setPost(postDTO);
        return request;
    }

    private static RequestPostProcessor jwtAs(UUID userId) {
        return jwt()
            .jwt(token -> token.subject(userId.toString()))
            .authorities(new SimpleGrantedAuthority(AuthoritiesConstants.USER));
    }
}
