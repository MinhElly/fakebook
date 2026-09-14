package com.minh.fakebook.comment.web.rest;

import static com.minh.fakebook.comment.domain.CommentReactionAsserts.*;
import static com.minh.fakebook.comment.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minh.fakebook.comment.IntegrationTest;
import com.minh.fakebook.comment.domain.Comment;
import com.minh.fakebook.comment.domain.CommentReaction;
import com.minh.fakebook.comment.domain.enumeration.ReactionType;
import com.minh.fakebook.comment.repository.CommentReactionRepository;
import com.minh.fakebook.comment.service.dto.CommentReactionDTO;
import com.minh.fakebook.comment.service.mapper.CommentReactionMapper;
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

/**
 * Integration tests for the {@link CommentReactionResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class CommentReactionResourceIT {

    private static final UUID DEFAULT_USER_ID = UUID.randomUUID();
    private static final UUID UPDATED_USER_ID = UUID.randomUUID();

    private static final ReactionType DEFAULT_REACTION_TYPE = ReactionType.LIKE;
    private static final ReactionType UPDATED_REACTION_TYPE = ReactionType.LOVE;

    private static final Instant DEFAULT_CREATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_CREATED_AT = Instant.ofEpochMilli(1703772585007L);

    private static final Instant DEFAULT_UPDATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_UPDATED_AT = Instant.ofEpochMilli(1703772585007L);

    private static final String ENTITY_API_URL = "/api/comment-reactions";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private CommentReactionRepository commentReactionRepository;

    @Autowired
    private CommentReactionMapper commentReactionMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restCommentReactionMockMvc;

    private CommentReaction commentReaction;

    private CommentReaction insertedCommentReaction;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static CommentReaction createEntity(EntityManager em) {
        CommentReaction commentReaction = new CommentReaction()
            .userId(DEFAULT_USER_ID)
            .reactionType(DEFAULT_REACTION_TYPE)
            .createdAt(DEFAULT_CREATED_AT)
            .updatedAt(DEFAULT_UPDATED_AT);
        // Add required entity
        Comment comment;
        if (TestUtil.findAll(em, Comment.class).isEmpty()) {
            comment = CommentResourceIT.createEntity();
            em.persist(comment);
            em.flush();
        } else {
            comment = TestUtil.findAll(em, Comment.class).get(0);
        }
        commentReaction.setComment(comment);
        return commentReaction;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static CommentReaction createUpdatedEntity(EntityManager em) {
        CommentReaction updatedCommentReaction = new CommentReaction()
            .userId(UPDATED_USER_ID)
            .reactionType(UPDATED_REACTION_TYPE)
            .createdAt(UPDATED_CREATED_AT)
            .updatedAt(UPDATED_UPDATED_AT);
        // Add required entity
        Comment comment;
        if (TestUtil.findAll(em, Comment.class).isEmpty()) {
            comment = CommentResourceIT.createUpdatedEntity();
            em.persist(comment);
            em.flush();
        } else {
            comment = TestUtil.findAll(em, Comment.class).get(0);
        }
        updatedCommentReaction.setComment(comment);
        return updatedCommentReaction;
    }

    @BeforeEach
    void initTest() {
        commentReaction = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedCommentReaction != null) {
            commentReactionRepository.delete(insertedCommentReaction);
            insertedCommentReaction = null;
        }
    }

    @Test
    @Transactional
    void createCommentReaction() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the CommentReaction
        CommentReactionDTO commentReactionDTO = commentReactionMapper.toDto(commentReaction);
        var returnedCommentReactionDTO = om.readValue(
            restCommentReactionMockMvc
                .perform(
                    post(ENTITY_API_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsBytes(commentReactionDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            CommentReactionDTO.class
        );

        // Validate the CommentReaction in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedCommentReaction = commentReactionMapper.toEntity(returnedCommentReactionDTO);
        assertCommentReactionUpdatableFieldsEquals(returnedCommentReaction, getPersistedCommentReaction(returnedCommentReaction));

        insertedCommentReaction = returnedCommentReaction;
    }

    @Test
    @Transactional
    void createCommentReactionWithExistingId() throws Exception {
        // Create the CommentReaction with an existing ID
        insertedCommentReaction = commentReactionRepository.saveAndFlush(commentReaction);
        CommentReactionDTO commentReactionDTO = commentReactionMapper.toDto(commentReaction);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restCommentReactionMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(commentReactionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the CommentReaction in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkUserIdIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        commentReaction.setUserId(null);

        // Create the CommentReaction, which fails.
        CommentReactionDTO commentReactionDTO = commentReactionMapper.toDto(commentReaction);

        restCommentReactionMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(commentReactionDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkReactionTypeIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        commentReaction.setReactionType(null);

        // Create the CommentReaction, which fails.
        CommentReactionDTO commentReactionDTO = commentReactionMapper.toDto(commentReaction);

        restCommentReactionMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(commentReactionDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkCreatedAtIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        commentReaction.setCreatedAt(null);

        // Create the CommentReaction, which fails.
        CommentReactionDTO commentReactionDTO = commentReactionMapper.toDto(commentReaction);

        restCommentReactionMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(commentReactionDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllCommentReactions() throws Exception {
        // Initialize the database
        insertedCommentReaction = commentReactionRepository.saveAndFlush(commentReaction);

        // Get all the commentReactionList
        restCommentReactionMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(commentReaction.getId().toString())))
            .andExpect(jsonPath("$.[*].userId").value(hasItem(DEFAULT_USER_ID.toString())))
            .andExpect(jsonPath("$.[*].reactionType").value(hasItem(DEFAULT_REACTION_TYPE.toString())))
            .andExpect(jsonPath("$.[*].createdAt").value(hasItem(DEFAULT_CREATED_AT.toString())))
            .andExpect(jsonPath("$.[*].updatedAt").value(hasItem(DEFAULT_UPDATED_AT.toString())));
    }

    @Test
    @Transactional
    void getCommentReaction() throws Exception {
        // Initialize the database
        insertedCommentReaction = commentReactionRepository.saveAndFlush(commentReaction);

        // Get the commentReaction
        restCommentReactionMockMvc
            .perform(get(ENTITY_API_URL_ID, commentReaction.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(commentReaction.getId().toString()))
            .andExpect(jsonPath("$.userId").value(DEFAULT_USER_ID.toString()))
            .andExpect(jsonPath("$.reactionType").value(DEFAULT_REACTION_TYPE.toString()))
            .andExpect(jsonPath("$.createdAt").value(DEFAULT_CREATED_AT.toString()))
            .andExpect(jsonPath("$.updatedAt").value(DEFAULT_UPDATED_AT.toString()));
    }

    @Test
    @Transactional
    void getCommentReactionsByIdFiltering() throws Exception {
        // Initialize the database
        insertedCommentReaction = commentReactionRepository.saveAndFlush(commentReaction);

        UUID id = commentReaction.getId();

        defaultCommentReactionFiltering("id.equals=" + id, "id.notEquals=" + id);
    }

    @Test
    @Transactional
    void getAllCommentReactionsByUserIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedCommentReaction = commentReactionRepository.saveAndFlush(commentReaction);

        // Get all the commentReactionList where userId equals to
        defaultCommentReactionFiltering("userId.equals=" + DEFAULT_USER_ID, "userId.equals=" + UPDATED_USER_ID);
    }

    @Test
    @Transactional
    void getAllCommentReactionsByUserIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedCommentReaction = commentReactionRepository.saveAndFlush(commentReaction);

        // Get all the commentReactionList where userId in
        defaultCommentReactionFiltering("userId.in=" + DEFAULT_USER_ID + "," + UPDATED_USER_ID, "userId.in=" + UPDATED_USER_ID);
    }

    @Test
    @Transactional
    void getAllCommentReactionsByUserIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedCommentReaction = commentReactionRepository.saveAndFlush(commentReaction);

        // Get all the commentReactionList where userId is not null
        defaultCommentReactionFiltering("userId.specified=true", "userId.specified=false");
    }

    @Test
    @Transactional
    void getAllCommentReactionsByReactionTypeIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedCommentReaction = commentReactionRepository.saveAndFlush(commentReaction);

        // Get all the commentReactionList where reactionType equals to
        defaultCommentReactionFiltering("reactionType.equals=" + DEFAULT_REACTION_TYPE, "reactionType.equals=" + UPDATED_REACTION_TYPE);
    }

    @Test
    @Transactional
    void getAllCommentReactionsByReactionTypeIsInShouldWork() throws Exception {
        // Initialize the database
        insertedCommentReaction = commentReactionRepository.saveAndFlush(commentReaction);

        // Get all the commentReactionList where reactionType in
        defaultCommentReactionFiltering(
            "reactionType.in=" + DEFAULT_REACTION_TYPE + "," + UPDATED_REACTION_TYPE,
            "reactionType.in=" + UPDATED_REACTION_TYPE
        );
    }

    @Test
    @Transactional
    void getAllCommentReactionsByReactionTypeIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedCommentReaction = commentReactionRepository.saveAndFlush(commentReaction);

        // Get all the commentReactionList where reactionType is not null
        defaultCommentReactionFiltering("reactionType.specified=true", "reactionType.specified=false");
    }

    @Test
    @Transactional
    void getAllCommentReactionsByCreatedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedCommentReaction = commentReactionRepository.saveAndFlush(commentReaction);

        // Get all the commentReactionList where createdAt equals to
        defaultCommentReactionFiltering("createdAt.equals=" + DEFAULT_CREATED_AT, "createdAt.equals=" + UPDATED_CREATED_AT);
    }

    @Test
    @Transactional
    void getAllCommentReactionsByCreatedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedCommentReaction = commentReactionRepository.saveAndFlush(commentReaction);

        // Get all the commentReactionList where createdAt in
        defaultCommentReactionFiltering(
            "createdAt.in=" + DEFAULT_CREATED_AT + "," + UPDATED_CREATED_AT,
            "createdAt.in=" + UPDATED_CREATED_AT
        );
    }

    @Test
    @Transactional
    void getAllCommentReactionsByCreatedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedCommentReaction = commentReactionRepository.saveAndFlush(commentReaction);

        // Get all the commentReactionList where createdAt is not null
        defaultCommentReactionFiltering("createdAt.specified=true", "createdAt.specified=false");
    }

    @Test
    @Transactional
    void getAllCommentReactionsByUpdatedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedCommentReaction = commentReactionRepository.saveAndFlush(commentReaction);

        // Get all the commentReactionList where updatedAt equals to
        defaultCommentReactionFiltering("updatedAt.equals=" + DEFAULT_UPDATED_AT, "updatedAt.equals=" + UPDATED_UPDATED_AT);
    }

    @Test
    @Transactional
    void getAllCommentReactionsByUpdatedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedCommentReaction = commentReactionRepository.saveAndFlush(commentReaction);

        // Get all the commentReactionList where updatedAt in
        defaultCommentReactionFiltering(
            "updatedAt.in=" + DEFAULT_UPDATED_AT + "," + UPDATED_UPDATED_AT,
            "updatedAt.in=" + UPDATED_UPDATED_AT
        );
    }

    @Test
    @Transactional
    void getAllCommentReactionsByUpdatedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedCommentReaction = commentReactionRepository.saveAndFlush(commentReaction);

        // Get all the commentReactionList where updatedAt is not null
        defaultCommentReactionFiltering("updatedAt.specified=true", "updatedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllCommentReactionsByCommentIsEqualToSomething() throws Exception {
        Comment comment;
        if (TestUtil.findAll(em, Comment.class).isEmpty()) {
            commentReactionRepository.saveAndFlush(commentReaction);
            comment = CommentResourceIT.createEntity();
        } else {
            comment = TestUtil.findAll(em, Comment.class).get(0);
        }
        em.persist(comment);
        em.flush();
        commentReaction.setComment(comment);
        commentReactionRepository.saveAndFlush(commentReaction);
        UUID commentId = comment.getId();
        // Get all the commentReactionList where comment equals to commentId
        defaultCommentReactionShouldBeFound("commentId.equals=" + commentId);

        // Get all the commentReactionList where comment equals to UUID.randomUUID()
        defaultCommentReactionShouldNotBeFound("commentId.equals=" + UUID.randomUUID());
    }

    private void defaultCommentReactionFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultCommentReactionShouldBeFound(shouldBeFound);
        defaultCommentReactionShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultCommentReactionShouldBeFound(String filter) throws Exception {
        restCommentReactionMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(commentReaction.getId().toString())))
            .andExpect(jsonPath("$.[*].userId").value(hasItem(DEFAULT_USER_ID.toString())))
            .andExpect(jsonPath("$.[*].reactionType").value(hasItem(DEFAULT_REACTION_TYPE.toString())))
            .andExpect(jsonPath("$.[*].createdAt").value(hasItem(DEFAULT_CREATED_AT.toString())))
            .andExpect(jsonPath("$.[*].updatedAt").value(hasItem(DEFAULT_UPDATED_AT.toString())));

        // Check, that the count call also returns 1
        restCommentReactionMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultCommentReactionShouldNotBeFound(String filter) throws Exception {
        restCommentReactionMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restCommentReactionMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingCommentReaction() throws Exception {
        // Get the commentReaction
        restCommentReactionMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingCommentReaction() throws Exception {
        // Initialize the database
        insertedCommentReaction = commentReactionRepository.saveAndFlush(commentReaction);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the commentReaction
        CommentReaction updatedCommentReaction = commentReactionRepository.findById(commentReaction.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedCommentReaction are not directly saved in db
        em.detach(updatedCommentReaction);
        updatedCommentReaction
            .userId(UPDATED_USER_ID)
            .reactionType(UPDATED_REACTION_TYPE)
            .createdAt(UPDATED_CREATED_AT)
            .updatedAt(UPDATED_UPDATED_AT);
        CommentReactionDTO commentReactionDTO = commentReactionMapper.toDto(updatedCommentReaction);

        restCommentReactionMockMvc
            .perform(
                put(ENTITY_API_URL_ID, commentReactionDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(commentReactionDTO))
            )
            .andExpect(status().isOk());

        // Validate the CommentReaction in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedCommentReactionToMatchAllProperties(updatedCommentReaction);
    }

    @Test
    @Transactional
    void putNonExistingCommentReaction() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        commentReaction.setId(UUID.randomUUID());

        // Create the CommentReaction
        CommentReactionDTO commentReactionDTO = commentReactionMapper.toDto(commentReaction);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restCommentReactionMockMvc
            .perform(
                put(ENTITY_API_URL_ID, commentReactionDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(commentReactionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the CommentReaction in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchCommentReaction() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        commentReaction.setId(UUID.randomUUID());

        // Create the CommentReaction
        CommentReactionDTO commentReactionDTO = commentReactionMapper.toDto(commentReaction);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restCommentReactionMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(commentReactionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the CommentReaction in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamCommentReaction() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        commentReaction.setId(UUID.randomUUID());

        // Create the CommentReaction
        CommentReactionDTO commentReactionDTO = commentReactionMapper.toDto(commentReaction);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restCommentReactionMockMvc
            .perform(
                put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(commentReactionDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the CommentReaction in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateCommentReactionWithPatch() throws Exception {
        // Initialize the database
        insertedCommentReaction = commentReactionRepository.saveAndFlush(commentReaction);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the commentReaction using partial update
        CommentReaction partialUpdatedCommentReaction = new CommentReaction();
        partialUpdatedCommentReaction.setId(commentReaction.getId());

        partialUpdatedCommentReaction.createdAt(UPDATED_CREATED_AT).updatedAt(UPDATED_UPDATED_AT);

        restCommentReactionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedCommentReaction.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedCommentReaction))
            )
            .andExpect(status().isOk());

        // Validate the CommentReaction in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertCommentReactionUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedCommentReaction, commentReaction),
            getPersistedCommentReaction(commentReaction)
        );
    }

    @Test
    @Transactional
    void fullUpdateCommentReactionWithPatch() throws Exception {
        // Initialize the database
        insertedCommentReaction = commentReactionRepository.saveAndFlush(commentReaction);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the commentReaction using partial update
        CommentReaction partialUpdatedCommentReaction = new CommentReaction();
        partialUpdatedCommentReaction.setId(commentReaction.getId());

        partialUpdatedCommentReaction
            .userId(UPDATED_USER_ID)
            .reactionType(UPDATED_REACTION_TYPE)
            .createdAt(UPDATED_CREATED_AT)
            .updatedAt(UPDATED_UPDATED_AT);

        restCommentReactionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedCommentReaction.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedCommentReaction))
            )
            .andExpect(status().isOk());

        // Validate the CommentReaction in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertCommentReactionUpdatableFieldsEquals(
            partialUpdatedCommentReaction,
            getPersistedCommentReaction(partialUpdatedCommentReaction)
        );
    }

    @Test
    @Transactional
    void patchNonExistingCommentReaction() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        commentReaction.setId(UUID.randomUUID());

        // Create the CommentReaction
        CommentReactionDTO commentReactionDTO = commentReactionMapper.toDto(commentReaction);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restCommentReactionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, commentReactionDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(commentReactionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the CommentReaction in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchCommentReaction() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        commentReaction.setId(UUID.randomUUID());

        // Create the CommentReaction
        CommentReactionDTO commentReactionDTO = commentReactionMapper.toDto(commentReaction);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restCommentReactionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(commentReactionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the CommentReaction in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamCommentReaction() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        commentReaction.setId(UUID.randomUUID());

        // Create the CommentReaction
        CommentReactionDTO commentReactionDTO = commentReactionMapper.toDto(commentReaction);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restCommentReactionMockMvc
            .perform(
                patch(ENTITY_API_URL)
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(commentReactionDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the CommentReaction in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteCommentReaction() throws Exception {
        // Initialize the database
        insertedCommentReaction = commentReactionRepository.saveAndFlush(commentReaction);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the commentReaction
        restCommentReactionMockMvc
            .perform(delete(ENTITY_API_URL_ID, commentReaction.getId().toString()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return commentReactionRepository.count();
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

    protected CommentReaction getPersistedCommentReaction(CommentReaction commentReaction) {
        return commentReactionRepository.findById(commentReaction.getId()).orElseThrow();
    }

    protected void assertPersistedCommentReactionToMatchAllProperties(CommentReaction expectedCommentReaction) {
        assertCommentReactionAllPropertiesEquals(expectedCommentReaction, getPersistedCommentReaction(expectedCommentReaction));
    }

    protected void assertPersistedCommentReactionToMatchUpdatableProperties(CommentReaction expectedCommentReaction) {
        assertCommentReactionAllUpdatablePropertiesEquals(expectedCommentReaction, getPersistedCommentReaction(expectedCommentReaction));
    }
}
