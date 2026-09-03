package com.minh.fakebook.post.web.rest;

import static com.minh.fakebook.post.domain.PostMediaAsserts.*;
import static com.minh.fakebook.post.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.minh.fakebook.post.IntegrationTest;
import com.minh.fakebook.post.domain.Post;
import com.minh.fakebook.post.domain.PostMedia;
import com.minh.fakebook.post.repository.PostMediaRepository;
import com.minh.fakebook.post.service.dto.PostMediaDTO;
import com.minh.fakebook.post.service.mapper.PostMediaMapper;
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
 * Integration tests for the {@link PostMediaResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class PostMediaResourceIT {

    private static final UUID DEFAULT_MEDIA_ID = UUID.randomUUID();
    private static final UUID UPDATED_MEDIA_ID = UUID.randomUUID();

    private static final Integer DEFAULT_DISPLAY_ORDER = 0;
    private static final Integer UPDATED_DISPLAY_ORDER = 1;
    private static final Integer SMALLER_DISPLAY_ORDER = 0 - 1;

    private static final Instant DEFAULT_CREATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_CREATED_AT = Instant.ofEpochMilli(1701546327878L);

    private static final String ENTITY_API_URL = "/api/post-medias";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private PostMediaRepository postMediaRepository;

    @Autowired
    private PostMediaMapper postMediaMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restPostMediaMockMvc;

    private PostMedia postMedia;

    private PostMedia insertedPostMedia;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static PostMedia createEntity(EntityManager em) {
        PostMedia postMedia = new PostMedia().mediaId(DEFAULT_MEDIA_ID).displayOrder(DEFAULT_DISPLAY_ORDER).createdAt(DEFAULT_CREATED_AT);
        // Add required entity
        Post post;
        if (TestUtil.findAll(em, Post.class).isEmpty()) {
            post = PostResourceIT.createEntity();
            em.persist(post);
            em.flush();
        } else {
            post = TestUtil.findAll(em, Post.class).get(0);
        }
        postMedia.setPost(post);
        return postMedia;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static PostMedia createUpdatedEntity(EntityManager em) {
        PostMedia updatedPostMedia = new PostMedia()
            .mediaId(UPDATED_MEDIA_ID)
            .displayOrder(UPDATED_DISPLAY_ORDER)
            .createdAt(UPDATED_CREATED_AT);
        // Add required entity
        Post post;
        if (TestUtil.findAll(em, Post.class).isEmpty()) {
            post = PostResourceIT.createUpdatedEntity();
            em.persist(post);
            em.flush();
        } else {
            post = TestUtil.findAll(em, Post.class).get(0);
        }
        updatedPostMedia.setPost(post);
        return updatedPostMedia;
    }

    @BeforeEach
    void initTest() {
        postMedia = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedPostMedia != null) {
            postMediaRepository.delete(insertedPostMedia);
            insertedPostMedia = null;
        }
    }

    @Test
    @Transactional
    void createPostMedia() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the PostMedia
        PostMediaDTO postMediaDTO = postMediaMapper.toDto(postMedia);
        var returnedPostMediaDTO = om.readValue(
            restPostMediaMockMvc
                .perform(
                    post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(postMediaDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            PostMediaDTO.class
        );

        // Validate the PostMedia in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedPostMedia = postMediaMapper.toEntity(returnedPostMediaDTO);
        assertPostMediaUpdatableFieldsEquals(returnedPostMedia, getPersistedPostMedia(returnedPostMedia));

        insertedPostMedia = returnedPostMedia;
    }

    @Test
    @Transactional
    void createPostMediaWithExistingId() throws Exception {
        // Create the PostMedia with an existing ID
        insertedPostMedia = postMediaRepository.saveAndFlush(postMedia);
        PostMediaDTO postMediaDTO = postMediaMapper.toDto(postMedia);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restPostMediaMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(postMediaDTO)))
            .andExpect(status().isBadRequest());

        // Validate the PostMedia in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkMediaIdIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        postMedia.setMediaId(null);

        // Create the PostMedia, which fails.
        PostMediaDTO postMediaDTO = postMediaMapper.toDto(postMedia);

        restPostMediaMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(postMediaDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkDisplayOrderIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        postMedia.setDisplayOrder(null);

        // Create the PostMedia, which fails.
        PostMediaDTO postMediaDTO = postMediaMapper.toDto(postMedia);

        restPostMediaMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(postMediaDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkCreatedAtIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        postMedia.setCreatedAt(null);

        // Create the PostMedia, which fails.
        PostMediaDTO postMediaDTO = postMediaMapper.toDto(postMedia);

        restPostMediaMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(postMediaDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllPostMedias() throws Exception {
        // Initialize the database
        insertedPostMedia = postMediaRepository.saveAndFlush(postMedia);

        // Get all the postMediaList
        restPostMediaMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(postMedia.getId().toString())))
            .andExpect(jsonPath("$.[*].mediaId").value(hasItem(DEFAULT_MEDIA_ID.toString())))
            .andExpect(jsonPath("$.[*].displayOrder").value(hasItem(DEFAULT_DISPLAY_ORDER)))
            .andExpect(jsonPath("$.[*].createdAt").value(hasItem(DEFAULT_CREATED_AT.toString())));
    }

    @Test
    @Transactional
    void getPostMedia() throws Exception {
        // Initialize the database
        insertedPostMedia = postMediaRepository.saveAndFlush(postMedia);

        // Get the postMedia
        restPostMediaMockMvc
            .perform(get(ENTITY_API_URL_ID, postMedia.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(postMedia.getId().toString()))
            .andExpect(jsonPath("$.mediaId").value(DEFAULT_MEDIA_ID.toString()))
            .andExpect(jsonPath("$.displayOrder").value(DEFAULT_DISPLAY_ORDER))
            .andExpect(jsonPath("$.createdAt").value(DEFAULT_CREATED_AT.toString()));
    }

    @Test
    @Transactional
    void getPostMediasByIdFiltering() throws Exception {
        // Initialize the database
        insertedPostMedia = postMediaRepository.saveAndFlush(postMedia);

        UUID id = postMedia.getId();

        defaultPostMediaFiltering("id.equals=" + id, "id.notEquals=" + id);
    }

    @Test
    @Transactional
    void getAllPostMediasByMediaIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedPostMedia = postMediaRepository.saveAndFlush(postMedia);

        // Get all the postMediaList where mediaId equals to
        defaultPostMediaFiltering("mediaId.equals=" + DEFAULT_MEDIA_ID, "mediaId.equals=" + UPDATED_MEDIA_ID);
    }

    @Test
    @Transactional
    void getAllPostMediasByMediaIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedPostMedia = postMediaRepository.saveAndFlush(postMedia);

        // Get all the postMediaList where mediaId in
        defaultPostMediaFiltering("mediaId.in=" + DEFAULT_MEDIA_ID + "," + UPDATED_MEDIA_ID, "mediaId.in=" + UPDATED_MEDIA_ID);
    }

    @Test
    @Transactional
    void getAllPostMediasByMediaIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedPostMedia = postMediaRepository.saveAndFlush(postMedia);

        // Get all the postMediaList where mediaId is not null
        defaultPostMediaFiltering("mediaId.specified=true", "mediaId.specified=false");
    }

    @Test
    @Transactional
    void getAllPostMediasByDisplayOrderIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedPostMedia = postMediaRepository.saveAndFlush(postMedia);

        // Get all the postMediaList where displayOrder equals to
        defaultPostMediaFiltering("displayOrder.equals=" + DEFAULT_DISPLAY_ORDER, "displayOrder.equals=" + UPDATED_DISPLAY_ORDER);
    }

    @Test
    @Transactional
    void getAllPostMediasByDisplayOrderIsInShouldWork() throws Exception {
        // Initialize the database
        insertedPostMedia = postMediaRepository.saveAndFlush(postMedia);

        // Get all the postMediaList where displayOrder in
        defaultPostMediaFiltering(
            "displayOrder.in=" + DEFAULT_DISPLAY_ORDER + "," + UPDATED_DISPLAY_ORDER,
            "displayOrder.in=" + UPDATED_DISPLAY_ORDER
        );
    }

    @Test
    @Transactional
    void getAllPostMediasByDisplayOrderIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedPostMedia = postMediaRepository.saveAndFlush(postMedia);

        // Get all the postMediaList where displayOrder is not null
        defaultPostMediaFiltering("displayOrder.specified=true", "displayOrder.specified=false");
    }

    @Test
    @Transactional
    void getAllPostMediasByDisplayOrderIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedPostMedia = postMediaRepository.saveAndFlush(postMedia);

        // Get all the postMediaList where displayOrder is greater than or equal to
        defaultPostMediaFiltering(
            "displayOrder.greaterThanOrEqual=" + DEFAULT_DISPLAY_ORDER,
            "displayOrder.greaterThanOrEqual=" + UPDATED_DISPLAY_ORDER
        );
    }

    @Test
    @Transactional
    void getAllPostMediasByDisplayOrderIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedPostMedia = postMediaRepository.saveAndFlush(postMedia);

        // Get all the postMediaList where displayOrder is less than or equal to
        defaultPostMediaFiltering(
            "displayOrder.lessThanOrEqual=" + DEFAULT_DISPLAY_ORDER,
            "displayOrder.lessThanOrEqual=" + SMALLER_DISPLAY_ORDER
        );
    }

    @Test
    @Transactional
    void getAllPostMediasByDisplayOrderIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedPostMedia = postMediaRepository.saveAndFlush(postMedia);

        // Get all the postMediaList where displayOrder is less than
        defaultPostMediaFiltering("displayOrder.lessThan=" + UPDATED_DISPLAY_ORDER, "displayOrder.lessThan=" + DEFAULT_DISPLAY_ORDER);
    }

    @Test
    @Transactional
    void getAllPostMediasByDisplayOrderIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedPostMedia = postMediaRepository.saveAndFlush(postMedia);

        // Get all the postMediaList where displayOrder is greater than
        defaultPostMediaFiltering("displayOrder.greaterThan=" + SMALLER_DISPLAY_ORDER, "displayOrder.greaterThan=" + DEFAULT_DISPLAY_ORDER);
    }

    @Test
    @Transactional
    void getAllPostMediasByCreatedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedPostMedia = postMediaRepository.saveAndFlush(postMedia);

        // Get all the postMediaList where createdAt equals to
        defaultPostMediaFiltering("createdAt.equals=" + DEFAULT_CREATED_AT, "createdAt.equals=" + UPDATED_CREATED_AT);
    }

    @Test
    @Transactional
    void getAllPostMediasByCreatedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedPostMedia = postMediaRepository.saveAndFlush(postMedia);

        // Get all the postMediaList where createdAt in
        defaultPostMediaFiltering("createdAt.in=" + DEFAULT_CREATED_AT + "," + UPDATED_CREATED_AT, "createdAt.in=" + UPDATED_CREATED_AT);
    }

    @Test
    @Transactional
    void getAllPostMediasByCreatedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedPostMedia = postMediaRepository.saveAndFlush(postMedia);

        // Get all the postMediaList where createdAt is not null
        defaultPostMediaFiltering("createdAt.specified=true", "createdAt.specified=false");
    }

    @Test
    @Transactional
    void getAllPostMediasByPostIsEqualToSomething() throws Exception {
        Post post;
        if (TestUtil.findAll(em, Post.class).isEmpty()) {
            postMediaRepository.saveAndFlush(postMedia);
            post = PostResourceIT.createEntity();
        } else {
            post = TestUtil.findAll(em, Post.class).get(0);
        }
        em.persist(post);
        em.flush();
        postMedia.setPost(post);
        postMediaRepository.saveAndFlush(postMedia);
        UUID postId = post.getId();
        // Get all the postMediaList where post equals to postId
        defaultPostMediaShouldBeFound("postId.equals=" + postId);

        // Get all the postMediaList where post equals to UUID.randomUUID()
        defaultPostMediaShouldNotBeFound("postId.equals=" + UUID.randomUUID());
    }

    private void defaultPostMediaFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultPostMediaShouldBeFound(shouldBeFound);
        defaultPostMediaShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultPostMediaShouldBeFound(String filter) throws Exception {
        restPostMediaMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(postMedia.getId().toString())))
            .andExpect(jsonPath("$.[*].mediaId").value(hasItem(DEFAULT_MEDIA_ID.toString())))
            .andExpect(jsonPath("$.[*].displayOrder").value(hasItem(DEFAULT_DISPLAY_ORDER)))
            .andExpect(jsonPath("$.[*].createdAt").value(hasItem(DEFAULT_CREATED_AT.toString())));

        // Check, that the count call also returns 1
        restPostMediaMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultPostMediaShouldNotBeFound(String filter) throws Exception {
        restPostMediaMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restPostMediaMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingPostMedia() throws Exception {
        // Get the postMedia
        restPostMediaMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingPostMedia() throws Exception {
        // Initialize the database
        insertedPostMedia = postMediaRepository.saveAndFlush(postMedia);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the postMedia
        PostMedia updatedPostMedia = postMediaRepository.findById(postMedia.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedPostMedia are not directly saved in db
        em.detach(updatedPostMedia);
        updatedPostMedia.mediaId(UPDATED_MEDIA_ID).displayOrder(UPDATED_DISPLAY_ORDER).createdAt(UPDATED_CREATED_AT);
        PostMediaDTO postMediaDTO = postMediaMapper.toDto(updatedPostMedia);

        restPostMediaMockMvc
            .perform(
                put(ENTITY_API_URL_ID, postMediaDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(postMediaDTO))
            )
            .andExpect(status().isOk());

        // Validate the PostMedia in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedPostMediaToMatchAllProperties(updatedPostMedia);
    }

    @Test
    @Transactional
    void putNonExistingPostMedia() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        postMedia.setId(UUID.randomUUID());

        // Create the PostMedia
        PostMediaDTO postMediaDTO = postMediaMapper.toDto(postMedia);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restPostMediaMockMvc
            .perform(
                put(ENTITY_API_URL_ID, postMediaDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(postMediaDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the PostMedia in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchPostMedia() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        postMedia.setId(UUID.randomUUID());

        // Create the PostMedia
        PostMediaDTO postMediaDTO = postMediaMapper.toDto(postMedia);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restPostMediaMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(postMediaDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the PostMedia in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamPostMedia() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        postMedia.setId(UUID.randomUUID());

        // Create the PostMedia
        PostMediaDTO postMediaDTO = postMediaMapper.toDto(postMedia);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restPostMediaMockMvc
            .perform(put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(postMediaDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the PostMedia in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdatePostMediaWithPatch() throws Exception {
        // Initialize the database
        insertedPostMedia = postMediaRepository.saveAndFlush(postMedia);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the postMedia using partial update
        PostMedia partialUpdatedPostMedia = new PostMedia();
        partialUpdatedPostMedia.setId(postMedia.getId());

        partialUpdatedPostMedia.mediaId(UPDATED_MEDIA_ID).createdAt(UPDATED_CREATED_AT);

        restPostMediaMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedPostMedia.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedPostMedia))
            )
            .andExpect(status().isOk());

        // Validate the PostMedia in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPostMediaUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedPostMedia, postMedia),
            getPersistedPostMedia(postMedia)
        );
    }

    @Test
    @Transactional
    void fullUpdatePostMediaWithPatch() throws Exception {
        // Initialize the database
        insertedPostMedia = postMediaRepository.saveAndFlush(postMedia);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the postMedia using partial update
        PostMedia partialUpdatedPostMedia = new PostMedia();
        partialUpdatedPostMedia.setId(postMedia.getId());

        partialUpdatedPostMedia.mediaId(UPDATED_MEDIA_ID).displayOrder(UPDATED_DISPLAY_ORDER).createdAt(UPDATED_CREATED_AT);

        restPostMediaMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedPostMedia.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedPostMedia))
            )
            .andExpect(status().isOk());

        // Validate the PostMedia in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPostMediaUpdatableFieldsEquals(partialUpdatedPostMedia, getPersistedPostMedia(partialUpdatedPostMedia));
    }

    @Test
    @Transactional
    void patchNonExistingPostMedia() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        postMedia.setId(UUID.randomUUID());

        // Create the PostMedia
        PostMediaDTO postMediaDTO = postMediaMapper.toDto(postMedia);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restPostMediaMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, postMediaDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(postMediaDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the PostMedia in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchPostMedia() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        postMedia.setId(UUID.randomUUID());

        // Create the PostMedia
        PostMediaDTO postMediaDTO = postMediaMapper.toDto(postMedia);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restPostMediaMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(postMediaDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the PostMedia in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamPostMedia() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        postMedia.setId(UUID.randomUUID());

        // Create the PostMedia
        PostMediaDTO postMediaDTO = postMediaMapper.toDto(postMedia);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restPostMediaMockMvc
            .perform(
                patch(ENTITY_API_URL).with(csrf()).contentType("application/merge-patch+json").content(om.writeValueAsBytes(postMediaDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the PostMedia in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deletePostMedia() throws Exception {
        // Initialize the database
        insertedPostMedia = postMediaRepository.saveAndFlush(postMedia);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the postMedia
        restPostMediaMockMvc
            .perform(delete(ENTITY_API_URL_ID, postMedia.getId().toString()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return postMediaRepository.count();
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

    protected PostMedia getPersistedPostMedia(PostMedia postMedia) {
        return postMediaRepository.findById(postMedia.getId()).orElseThrow();
    }

    protected void assertPersistedPostMediaToMatchAllProperties(PostMedia expectedPostMedia) {
        assertPostMediaAllPropertiesEquals(expectedPostMedia, getPersistedPostMedia(expectedPostMedia));
    }

    protected void assertPersistedPostMediaToMatchUpdatableProperties(PostMedia expectedPostMedia) {
        assertPostMediaAllUpdatablePropertiesEquals(expectedPostMedia, getPersistedPostMedia(expectedPostMedia));
    }
}
