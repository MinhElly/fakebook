package com.minh.fakebook.post.web.rest;

import static com.minh.fakebook.post.domain.PostReactionAsserts.*;
import static com.minh.fakebook.post.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.minh.fakebook.post.IntegrationTest;
import com.minh.fakebook.post.domain.Post;
import com.minh.fakebook.post.domain.PostReaction;
import com.minh.fakebook.post.domain.enumeration.ReactionType;
import com.minh.fakebook.post.repository.PostReactionRepository;
import com.minh.fakebook.post.service.dto.PostReactionDTO;
import com.minh.fakebook.post.service.mapper.PostReactionMapper;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * Integration tests for the {@link PostReactionResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class PostReactionResourceIT {

    private static final UUID DEFAULT_USER_ID = UUID.randomUUID();
    private static final UUID UPDATED_USER_ID = UUID.randomUUID();

    private static final ReactionType DEFAULT_REACTION_TYPE = ReactionType.LIKE;
    private static final ReactionType UPDATED_REACTION_TYPE = ReactionType.LOVE;

    private static final Instant DEFAULT_CREATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_CREATED_AT = Instant.ofEpochMilli(1701546327878L);

    private static final Instant DEFAULT_UPDATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_UPDATED_AT = Instant.ofEpochMilli(1701546327878L);

    private static final String ENTITY_API_URL = "/api/post-reactions";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private PostReactionRepository postReactionRepository;

    @Autowired
    private PostReactionMapper postReactionMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restPostReactionMockMvc;

    private PostReaction postReaction;

    private PostReaction insertedPostReaction;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static PostReaction createEntity(EntityManager em) {
        PostReaction postReaction = new PostReaction()
            .userId(DEFAULT_USER_ID)
            .reactionType(DEFAULT_REACTION_TYPE)
            .createdAt(DEFAULT_CREATED_AT)
            .updatedAt(DEFAULT_UPDATED_AT);
        // Add required entity
        Post post;
        if (TestUtil.findAll(em, Post.class).isEmpty()) {
            post = PostResourceIT.createEntity();
            em.persist(post);
            em.flush();
        } else {
            post = TestUtil.findAll(em, Post.class).get(0);
        }
        postReaction.setPost(post);
        return postReaction;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static PostReaction createUpdatedEntity(EntityManager em) {
        PostReaction updatedPostReaction = new PostReaction()
            .userId(UPDATED_USER_ID)
            .reactionType(UPDATED_REACTION_TYPE)
            .createdAt(UPDATED_CREATED_AT)
            .updatedAt(UPDATED_UPDATED_AT);
        // Add required entity
        Post post;
        if (TestUtil.findAll(em, Post.class).isEmpty()) {
            post = PostResourceIT.createUpdatedEntity();
            em.persist(post);
            em.flush();
        } else {
            post = TestUtil.findAll(em, Post.class).get(0);
        }
        updatedPostReaction.setPost(post);
        return updatedPostReaction;
    }

    @BeforeEach
    void initTest() {
        postReaction = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedPostReaction != null) {
            postReactionRepository.delete(insertedPostReaction);
            insertedPostReaction = null;
        }
    }

    @Test
    @Transactional
    void createPostReaction() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the PostReaction
        PostReactionDTO postReactionDTO = postReactionMapper.toDto(postReaction);
        var returnedPostReactionDTO = om.readValue(
            restPostReactionMockMvc
                .perform(
                    post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(postReactionDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            PostReactionDTO.class
        );

        // Validate the PostReaction in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedPostReaction = postReactionMapper.toEntity(returnedPostReactionDTO);
        assertPostReactionUpdatableFieldsEquals(returnedPostReaction, getPersistedPostReaction(returnedPostReaction));

        insertedPostReaction = returnedPostReaction;
    }

    @Test
    @Transactional
    void createPostReactionWithExistingId() throws Exception {
        // Create the PostReaction with an existing ID
        insertedPostReaction = postReactionRepository.saveAndFlush(postReaction);
        PostReactionDTO postReactionDTO = postReactionMapper.toDto(postReaction);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restPostReactionMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(postReactionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the PostReaction in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkUserIdIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        postReaction.setUserId(null);

        // Create the PostReaction, which fails.
        PostReactionDTO postReactionDTO = postReactionMapper.toDto(postReaction);

        restPostReactionMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(postReactionDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkReactionTypeIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        postReaction.setReactionType(null);

        // Create the PostReaction, which fails.
        PostReactionDTO postReactionDTO = postReactionMapper.toDto(postReaction);

        restPostReactionMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(postReactionDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkCreatedAtIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        postReaction.setCreatedAt(null);

        // Create the PostReaction, which fails.
        PostReactionDTO postReactionDTO = postReactionMapper.toDto(postReaction);

        restPostReactionMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(postReactionDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllPostReactions() throws Exception {
        // Initialize the database
        insertedPostReaction = postReactionRepository.saveAndFlush(postReaction);

        // Get all the postReactionList
        restPostReactionMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(postReaction.getId().toString())))
            .andExpect(jsonPath("$.[*].userId").value(hasItem(DEFAULT_USER_ID.toString())))
            .andExpect(jsonPath("$.[*].reactionType").value(hasItem(DEFAULT_REACTION_TYPE.toString())))
            .andExpect(jsonPath("$.[*].createdAt").value(hasItem(DEFAULT_CREATED_AT.toString())))
            .andExpect(jsonPath("$.[*].updatedAt").value(hasItem(DEFAULT_UPDATED_AT.toString())));
    }

    @Test
    @Transactional
    void getPostReaction() throws Exception {
        // Initialize the database
        insertedPostReaction = postReactionRepository.saveAndFlush(postReaction);

        // Get the postReaction
        restPostReactionMockMvc
            .perform(get(ENTITY_API_URL_ID, postReaction.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(postReaction.getId().toString()))
            .andExpect(jsonPath("$.userId").value(DEFAULT_USER_ID.toString()))
            .andExpect(jsonPath("$.reactionType").value(DEFAULT_REACTION_TYPE.toString()))
            .andExpect(jsonPath("$.createdAt").value(DEFAULT_CREATED_AT.toString()))
            .andExpect(jsonPath("$.updatedAt").value(DEFAULT_UPDATED_AT.toString()));
    }

    @Test
    @Transactional
    void getPostReactionsByIdFiltering() throws Exception {
        // Initialize the database
        insertedPostReaction = postReactionRepository.saveAndFlush(postReaction);

        UUID id = postReaction.getId();

        defaultPostReactionFiltering("id.equals=" + id, "id.notEquals=" + id);
    }

    @Test
    @Transactional
    void getAllPostReactionsByUserIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedPostReaction = postReactionRepository.saveAndFlush(postReaction);

        // Get all the postReactionList where userId equals to
        defaultPostReactionFiltering("userId.equals=" + DEFAULT_USER_ID, "userId.equals=" + UPDATED_USER_ID);
    }

    @Test
    @Transactional
    void getAllPostReactionsByUserIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedPostReaction = postReactionRepository.saveAndFlush(postReaction);

        // Get all the postReactionList where userId in
        defaultPostReactionFiltering("userId.in=" + DEFAULT_USER_ID + "," + UPDATED_USER_ID, "userId.in=" + UPDATED_USER_ID);
    }

    @Test
    @Transactional
    void getAllPostReactionsByUserIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedPostReaction = postReactionRepository.saveAndFlush(postReaction);

        // Get all the postReactionList where userId is not null
        defaultPostReactionFiltering("userId.specified=true", "userId.specified=false");
    }

    @Test
    @Transactional
    void getAllPostReactionsByReactionTypeIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedPostReaction = postReactionRepository.saveAndFlush(postReaction);

        // Get all the postReactionList where reactionType equals to
        defaultPostReactionFiltering("reactionType.equals=" + DEFAULT_REACTION_TYPE, "reactionType.equals=" + UPDATED_REACTION_TYPE);
    }

    @Test
    @Transactional
    void getAllPostReactionsByReactionTypeIsInShouldWork() throws Exception {
        // Initialize the database
        insertedPostReaction = postReactionRepository.saveAndFlush(postReaction);

        // Get all the postReactionList where reactionType in
        defaultPostReactionFiltering(
            "reactionType.in=" + DEFAULT_REACTION_TYPE + "," + UPDATED_REACTION_TYPE,
            "reactionType.in=" + UPDATED_REACTION_TYPE
        );
    }

    @Test
    @Transactional
    void getAllPostReactionsByReactionTypeIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedPostReaction = postReactionRepository.saveAndFlush(postReaction);

        // Get all the postReactionList where reactionType is not null
        defaultPostReactionFiltering("reactionType.specified=true", "reactionType.specified=false");
    }

    @Test
    @Transactional
    void getAllPostReactionsByCreatedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedPostReaction = postReactionRepository.saveAndFlush(postReaction);

        // Get all the postReactionList where createdAt equals to
        defaultPostReactionFiltering("createdAt.equals=" + DEFAULT_CREATED_AT, "createdAt.equals=" + UPDATED_CREATED_AT);
    }

    @Test
    @Transactional
    void getAllPostReactionsByCreatedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedPostReaction = postReactionRepository.saveAndFlush(postReaction);

        // Get all the postReactionList where createdAt in
        defaultPostReactionFiltering("createdAt.in=" + DEFAULT_CREATED_AT + "," + UPDATED_CREATED_AT, "createdAt.in=" + UPDATED_CREATED_AT);
    }

    @Test
    @Transactional
    void getAllPostReactionsByCreatedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedPostReaction = postReactionRepository.saveAndFlush(postReaction);

        // Get all the postReactionList where createdAt is not null
        defaultPostReactionFiltering("createdAt.specified=true", "createdAt.specified=false");
    }

    @Test
    @Transactional
    void getAllPostReactionsByUpdatedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedPostReaction = postReactionRepository.saveAndFlush(postReaction);

        // Get all the postReactionList where updatedAt equals to
        defaultPostReactionFiltering("updatedAt.equals=" + DEFAULT_UPDATED_AT, "updatedAt.equals=" + UPDATED_UPDATED_AT);
    }

    @Test
    @Transactional
    void getAllPostReactionsByUpdatedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedPostReaction = postReactionRepository.saveAndFlush(postReaction);

        // Get all the postReactionList where updatedAt in
        defaultPostReactionFiltering("updatedAt.in=" + DEFAULT_UPDATED_AT + "," + UPDATED_UPDATED_AT, "updatedAt.in=" + UPDATED_UPDATED_AT);
    }

    @Test
    @Transactional
    void getAllPostReactionsByUpdatedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedPostReaction = postReactionRepository.saveAndFlush(postReaction);

        // Get all the postReactionList where updatedAt is not null
        defaultPostReactionFiltering("updatedAt.specified=true", "updatedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllPostReactionsByPostIsEqualToSomething() throws Exception {
        Post post;
        if (TestUtil.findAll(em, Post.class).isEmpty()) {
            postReactionRepository.saveAndFlush(postReaction);
            post = PostResourceIT.createEntity();
        } else {
            post = TestUtil.findAll(em, Post.class).get(0);
        }
        em.persist(post);
        em.flush();
        postReaction.setPost(post);
        postReactionRepository.saveAndFlush(postReaction);
        UUID postId = post.getId();
        // Get all the postReactionList where post equals to postId
        defaultPostReactionShouldBeFound("postId.equals=" + postId);

        // Get all the postReactionList where post equals to UUID.randomUUID()
        defaultPostReactionShouldNotBeFound("postId.equals=" + UUID.randomUUID());
    }

    private void defaultPostReactionFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultPostReactionShouldBeFound(shouldBeFound);
        defaultPostReactionShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultPostReactionShouldBeFound(String filter) throws Exception {
        restPostReactionMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(postReaction.getId().toString())))
            .andExpect(jsonPath("$.[*].userId").value(hasItem(DEFAULT_USER_ID.toString())))
            .andExpect(jsonPath("$.[*].reactionType").value(hasItem(DEFAULT_REACTION_TYPE.toString())))
            .andExpect(jsonPath("$.[*].createdAt").value(hasItem(DEFAULT_CREATED_AT.toString())))
            .andExpect(jsonPath("$.[*].updatedAt").value(hasItem(DEFAULT_UPDATED_AT.toString())));

        // Check, that the count call also returns 1
        restPostReactionMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultPostReactionShouldNotBeFound(String filter) throws Exception {
        restPostReactionMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restPostReactionMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingPostReaction() throws Exception {
        // Get the postReaction
        restPostReactionMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingPostReaction() throws Exception {
        // Initialize the database
        insertedPostReaction = postReactionRepository.saveAndFlush(postReaction);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the postReaction
        PostReaction updatedPostReaction = postReactionRepository.findById(postReaction.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedPostReaction are not directly saved in db
        em.detach(updatedPostReaction);
        updatedPostReaction
            .userId(UPDATED_USER_ID)
            .reactionType(UPDATED_REACTION_TYPE)
            .createdAt(UPDATED_CREATED_AT)
            .updatedAt(UPDATED_UPDATED_AT);
        PostReactionDTO postReactionDTO = postReactionMapper.toDto(updatedPostReaction);

        restPostReactionMockMvc
            .perform(
                put(ENTITY_API_URL_ID, postReactionDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(postReactionDTO))
            )
            .andExpect(status().isOk());

        // Validate the PostReaction in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedPostReactionToMatchAllProperties(updatedPostReaction);
    }

    @Test
    @Transactional
    void putNonExistingPostReaction() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        postReaction.setId(UUID.randomUUID());

        // Create the PostReaction
        PostReactionDTO postReactionDTO = postReactionMapper.toDto(postReaction);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restPostReactionMockMvc
            .perform(
                put(ENTITY_API_URL_ID, postReactionDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(postReactionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the PostReaction in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchPostReaction() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        postReaction.setId(UUID.randomUUID());

        // Create the PostReaction
        PostReactionDTO postReactionDTO = postReactionMapper.toDto(postReaction);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restPostReactionMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(postReactionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the PostReaction in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamPostReaction() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        postReaction.setId(UUID.randomUUID());

        // Create the PostReaction
        PostReactionDTO postReactionDTO = postReactionMapper.toDto(postReaction);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restPostReactionMockMvc
            .perform(
                put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(postReactionDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the PostReaction in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdatePostReactionWithPatch() throws Exception {
        // Initialize the database
        insertedPostReaction = postReactionRepository.saveAndFlush(postReaction);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the postReaction using partial update
        PostReaction partialUpdatedPostReaction = new PostReaction();
        partialUpdatedPostReaction.setId(postReaction.getId());

        partialUpdatedPostReaction.userId(UPDATED_USER_ID).reactionType(UPDATED_REACTION_TYPE).updatedAt(UPDATED_UPDATED_AT);

        restPostReactionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedPostReaction.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedPostReaction))
            )
            .andExpect(status().isOk());

        // Validate the PostReaction in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPostReactionUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedPostReaction, postReaction),
            getPersistedPostReaction(postReaction)
        );
    }

    @Test
    @Transactional
    void fullUpdatePostReactionWithPatch() throws Exception {
        // Initialize the database
        insertedPostReaction = postReactionRepository.saveAndFlush(postReaction);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the postReaction using partial update
        PostReaction partialUpdatedPostReaction = new PostReaction();
        partialUpdatedPostReaction.setId(postReaction.getId());

        partialUpdatedPostReaction
            .userId(UPDATED_USER_ID)
            .reactionType(UPDATED_REACTION_TYPE)
            .createdAt(UPDATED_CREATED_AT)
            .updatedAt(UPDATED_UPDATED_AT);

        restPostReactionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedPostReaction.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedPostReaction))
            )
            .andExpect(status().isOk());

        // Validate the PostReaction in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPostReactionUpdatableFieldsEquals(partialUpdatedPostReaction, getPersistedPostReaction(partialUpdatedPostReaction));
    }

    @Test
    @Transactional
    void patchNonExistingPostReaction() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        postReaction.setId(UUID.randomUUID());

        // Create the PostReaction
        PostReactionDTO postReactionDTO = postReactionMapper.toDto(postReaction);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restPostReactionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, postReactionDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(postReactionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the PostReaction in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchPostReaction() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        postReaction.setId(UUID.randomUUID());

        // Create the PostReaction
        PostReactionDTO postReactionDTO = postReactionMapper.toDto(postReaction);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restPostReactionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(postReactionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the PostReaction in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamPostReaction() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        postReaction.setId(UUID.randomUUID());

        // Create the PostReaction
        PostReactionDTO postReactionDTO = postReactionMapper.toDto(postReaction);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restPostReactionMockMvc
            .perform(
                patch(ENTITY_API_URL)
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(postReactionDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the PostReaction in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deletePostReaction() throws Exception {
        // Initialize the database
        insertedPostReaction = postReactionRepository.saveAndFlush(postReaction);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the postReaction
        restPostReactionMockMvc
            .perform(delete(ENTITY_API_URL_ID, postReaction.getId().toString()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return postReactionRepository.count();
    }

    protected void assertIncrementedRepositoryCount(long countBefore) {
        assertThat(countBefore + 1).isEqualTo(getRepositoryCount());
    }

    protected void assertDecrementedRepositoryCount(long countBefore) {
        assertThat(countBefore - 1).isEqualTo(getRepositoryCount());
    }

    protected void assertSameRepositoryCount(long countBefore) {
        assertThat(countBefore).isEqualTo(getRepositoryCount());
    }

    protected PostReaction getPersistedPostReaction(PostReaction postReaction) {
        return postReactionRepository.findById(postReaction.getId()).orElseThrow();
    }

    protected void assertPersistedPostReactionToMatchAllProperties(PostReaction expectedPostReaction) {
        assertPostReactionAllPropertiesEquals(expectedPostReaction, getPersistedPostReaction(expectedPostReaction));
    }

    protected void assertPersistedPostReactionToMatchUpdatableProperties(PostReaction expectedPostReaction) {
        assertPostReactionAllUpdatablePropertiesEquals(expectedPostReaction, getPersistedPostReaction(expectedPostReaction));
    }
}
