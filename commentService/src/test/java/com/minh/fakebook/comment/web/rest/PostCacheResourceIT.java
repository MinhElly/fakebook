package com.minh.fakebook.comment.web.rest;

import static com.minh.fakebook.comment.domain.PostCacheAsserts.*;
import static com.minh.fakebook.comment.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minh.fakebook.comment.IntegrationTest;
import com.minh.fakebook.comment.domain.PostCache;
import com.minh.fakebook.comment.repository.PostCacheRepository;
import jakarta.persistence.EntityManager;
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
 * Integration tests for the {@link PostCacheResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class PostCacheResourceIT {

    private static final UUID DEFAULT_AUTHOR_ID = UUID.randomUUID();
    private static final UUID UPDATED_AUTHOR_ID = UUID.randomUUID();

    private static final String DEFAULT_VISIBILITY = "AAAAAAAAAA";
    private static final String UPDATED_VISIBILITY = "BBBBBBBBBB";

    private static final String DEFAULT_STATUS = "AAAAAAAAAA";
    private static final String UPDATED_STATUS = "BBBBBBBBBB";

    private static final String ENTITY_API_URL = "/api/post-caches";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private PostCacheRepository postCacheRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restPostCacheMockMvc;

    private PostCache postCache;

    private PostCache insertedPostCache;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static PostCache createEntity() {
        return new PostCache().authorId(DEFAULT_AUTHOR_ID).visibility(DEFAULT_VISIBILITY).status(DEFAULT_STATUS);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static PostCache createUpdatedEntity() {
        return new PostCache().authorId(UPDATED_AUTHOR_ID).visibility(UPDATED_VISIBILITY).status(UPDATED_STATUS);
    }

    @BeforeEach
    void initTest() {
        postCache = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedPostCache != null) {
            postCacheRepository.delete(insertedPostCache);
            insertedPostCache = null;
        }
    }

    @Test
    @Transactional
    void createPostCache() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the PostCache
        var returnedPostCache = om.readValue(
            restPostCacheMockMvc
                .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(postCache)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            PostCache.class
        );

        // Validate the PostCache in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        assertPostCacheUpdatableFieldsEquals(returnedPostCache, getPersistedPostCache(returnedPostCache));

        insertedPostCache = returnedPostCache;
    }

    @Test
    @Transactional
    void createPostCacheWithExistingId() throws Exception {
        // Create the PostCache with an existing ID
        insertedPostCache = postCacheRepository.saveAndFlush(postCache);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restPostCacheMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(postCache)))
            .andExpect(status().isBadRequest());

        // Validate the PostCache in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkAuthorIdIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        postCache.setAuthorId(null);

        // Create the PostCache, which fails.

        restPostCacheMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(postCache)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkVisibilityIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        postCache.setVisibility(null);

        // Create the PostCache, which fails.

        restPostCacheMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(postCache)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkStatusIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        postCache.setStatus(null);

        // Create the PostCache, which fails.

        restPostCacheMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(postCache)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllPostCaches() throws Exception {
        // Initialize the database
        insertedPostCache = postCacheRepository.saveAndFlush(postCache);

        // Get all the postCacheList
        restPostCacheMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(postCache.getId().toString())))
            .andExpect(jsonPath("$.[*].authorId").value(hasItem(DEFAULT_AUTHOR_ID.toString())))
            .andExpect(jsonPath("$.[*].visibility").value(hasItem(DEFAULT_VISIBILITY)))
            .andExpect(jsonPath("$.[*].status").value(hasItem(DEFAULT_STATUS)));
    }

    @Test
    @Transactional
    void getPostCache() throws Exception {
        // Initialize the database
        insertedPostCache = postCacheRepository.saveAndFlush(postCache);

        // Get the postCache
        restPostCacheMockMvc
            .perform(get(ENTITY_API_URL_ID, postCache.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(postCache.getId().toString()))
            .andExpect(jsonPath("$.authorId").value(DEFAULT_AUTHOR_ID.toString()))
            .andExpect(jsonPath("$.visibility").value(DEFAULT_VISIBILITY))
            .andExpect(jsonPath("$.status").value(DEFAULT_STATUS));
    }

    @Test
    @Transactional
    void getNonExistingPostCache() throws Exception {
        // Get the postCache
        restPostCacheMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingPostCache() throws Exception {
        // Initialize the database
        insertedPostCache = postCacheRepository.saveAndFlush(postCache);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the postCache
        PostCache updatedPostCache = postCacheRepository.findById(postCache.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedPostCache are not directly saved in db
        em.detach(updatedPostCache);
        updatedPostCache.authorId(UPDATED_AUTHOR_ID).visibility(UPDATED_VISIBILITY).status(UPDATED_STATUS);

        restPostCacheMockMvc
            .perform(
                put(ENTITY_API_URL_ID, updatedPostCache.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(updatedPostCache))
            )
            .andExpect(status().isOk());

        // Validate the PostCache in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedPostCacheToMatchAllProperties(updatedPostCache);
    }

    @Test
    @Transactional
    void putNonExistingPostCache() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        postCache.setId(UUID.randomUUID());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restPostCacheMockMvc
            .perform(
                put(ENTITY_API_URL_ID, postCache.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(postCache))
            )
            .andExpect(status().isBadRequest());

        // Validate the PostCache in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchPostCache() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        postCache.setId(UUID.randomUUID());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restPostCacheMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(postCache))
            )
            .andExpect(status().isBadRequest());

        // Validate the PostCache in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamPostCache() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        postCache.setId(UUID.randomUUID());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restPostCacheMockMvc
            .perform(put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(postCache)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the PostCache in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdatePostCacheWithPatch() throws Exception {
        // Initialize the database
        insertedPostCache = postCacheRepository.saveAndFlush(postCache);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the postCache using partial update
        PostCache partialUpdatedPostCache = new PostCache();
        partialUpdatedPostCache.setId(postCache.getId());

        partialUpdatedPostCache.authorId(UPDATED_AUTHOR_ID).status(UPDATED_STATUS);

        restPostCacheMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedPostCache.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedPostCache))
            )
            .andExpect(status().isOk());

        // Validate the PostCache in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPostCacheUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedPostCache, postCache),
            getPersistedPostCache(postCache)
        );
    }

    @Test
    @Transactional
    void fullUpdatePostCacheWithPatch() throws Exception {
        // Initialize the database
        insertedPostCache = postCacheRepository.saveAndFlush(postCache);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the postCache using partial update
        PostCache partialUpdatedPostCache = new PostCache();
        partialUpdatedPostCache.setId(postCache.getId());

        partialUpdatedPostCache.authorId(UPDATED_AUTHOR_ID).visibility(UPDATED_VISIBILITY).status(UPDATED_STATUS);

        restPostCacheMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedPostCache.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedPostCache))
            )
            .andExpect(status().isOk());

        // Validate the PostCache in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPostCacheUpdatableFieldsEquals(partialUpdatedPostCache, getPersistedPostCache(partialUpdatedPostCache));
    }

    @Test
    @Transactional
    void patchNonExistingPostCache() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        postCache.setId(UUID.randomUUID());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restPostCacheMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, postCache.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(postCache))
            )
            .andExpect(status().isBadRequest());

        // Validate the PostCache in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchPostCache() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        postCache.setId(UUID.randomUUID());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restPostCacheMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(postCache))
            )
            .andExpect(status().isBadRequest());

        // Validate the PostCache in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamPostCache() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        postCache.setId(UUID.randomUUID());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restPostCacheMockMvc
            .perform(
                patch(ENTITY_API_URL).with(csrf()).contentType("application/merge-patch+json").content(om.writeValueAsBytes(postCache))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the PostCache in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deletePostCache() throws Exception {
        // Initialize the database
        insertedPostCache = postCacheRepository.saveAndFlush(postCache);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the postCache
        restPostCacheMockMvc
            .perform(delete(ENTITY_API_URL_ID, postCache.getId().toString()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return postCacheRepository.count();
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

    protected PostCache getPersistedPostCache(PostCache postCache) {
        return postCacheRepository.findById(postCache.getId()).orElseThrow();
    }

    protected void assertPersistedPostCacheToMatchAllProperties(PostCache expectedPostCache) {
        assertPostCacheAllPropertiesEquals(expectedPostCache, getPersistedPostCache(expectedPostCache));
    }

    protected void assertPersistedPostCacheToMatchUpdatableProperties(PostCache expectedPostCache) {
        assertPostCacheAllUpdatablePropertiesEquals(expectedPostCache, getPersistedPostCache(expectedPostCache));
    }
}
