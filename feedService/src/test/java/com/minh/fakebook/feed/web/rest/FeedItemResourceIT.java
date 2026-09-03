package com.minh.fakebook.feed.web.rest;

import static com.minh.fakebook.feed.domain.FeedItemAsserts.*;
import static com.minh.fakebook.feed.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.minh.fakebook.feed.IntegrationTest;
import com.minh.fakebook.feed.domain.FeedItem;
import com.minh.fakebook.feed.repository.FeedItemRepository;
import com.minh.fakebook.feed.service.dto.FeedItemDTO;
import com.minh.fakebook.feed.service.mapper.FeedItemMapper;
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
 * Integration tests for the {@link FeedItemResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class FeedItemResourceIT {

    private static final UUID DEFAULT_USER_ID = UUID.randomUUID();
    private static final UUID UPDATED_USER_ID = UUID.randomUUID();

    private static final UUID DEFAULT_POST_ID = UUID.randomUUID();
    private static final UUID UPDATED_POST_ID = UUID.randomUUID();

    private static final Instant DEFAULT_CREATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_CREATED_AT = Instant.ofEpochMilli(1703005872440L);

    private static final String ENTITY_API_URL = "/api/feed-items";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private FeedItemRepository feedItemRepository;

    @Autowired
    private FeedItemMapper feedItemMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restFeedItemMockMvc;

    private FeedItem feedItem;

    private FeedItem insertedFeedItem;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static FeedItem createEntity() {
        return new FeedItem().userId(DEFAULT_USER_ID).postId(DEFAULT_POST_ID).createdAt(DEFAULT_CREATED_AT);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static FeedItem createUpdatedEntity() {
        return new FeedItem().userId(UPDATED_USER_ID).postId(UPDATED_POST_ID).createdAt(UPDATED_CREATED_AT);
    }

    @BeforeEach
    void initTest() {
        feedItem = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedFeedItem != null) {
            feedItemRepository.delete(insertedFeedItem);
            insertedFeedItem = null;
        }
    }

    @Test
    @Transactional
    void createFeedItem() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the FeedItem
        FeedItemDTO feedItemDTO = feedItemMapper.toDto(feedItem);
        var returnedFeedItemDTO = om.readValue(
            restFeedItemMockMvc
                .perform(
                    post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(feedItemDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            FeedItemDTO.class
        );

        // Validate the FeedItem in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedFeedItem = feedItemMapper.toEntity(returnedFeedItemDTO);
        assertFeedItemUpdatableFieldsEquals(returnedFeedItem, getPersistedFeedItem(returnedFeedItem));

        insertedFeedItem = returnedFeedItem;
    }

    @Test
    @Transactional
    void createFeedItemWithExistingId() throws Exception {
        // Create the FeedItem with an existing ID
        insertedFeedItem = feedItemRepository.saveAndFlush(feedItem);
        FeedItemDTO feedItemDTO = feedItemMapper.toDto(feedItem);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restFeedItemMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(feedItemDTO)))
            .andExpect(status().isBadRequest());

        // Validate the FeedItem in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkUserIdIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        feedItem.setUserId(null);

        // Create the FeedItem, which fails.
        FeedItemDTO feedItemDTO = feedItemMapper.toDto(feedItem);

        restFeedItemMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(feedItemDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkPostIdIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        feedItem.setPostId(null);

        // Create the FeedItem, which fails.
        FeedItemDTO feedItemDTO = feedItemMapper.toDto(feedItem);

        restFeedItemMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(feedItemDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkCreatedAtIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        feedItem.setCreatedAt(null);

        // Create the FeedItem, which fails.
        FeedItemDTO feedItemDTO = feedItemMapper.toDto(feedItem);

        restFeedItemMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(feedItemDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllFeedItems() throws Exception {
        // Initialize the database
        insertedFeedItem = feedItemRepository.saveAndFlush(feedItem);

        // Get all the feedItemList
        restFeedItemMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(feedItem.getId().toString())))
            .andExpect(jsonPath("$.[*].userId").value(hasItem(DEFAULT_USER_ID.toString())))
            .andExpect(jsonPath("$.[*].postId").value(hasItem(DEFAULT_POST_ID.toString())))
            .andExpect(jsonPath("$.[*].createdAt").value(hasItem(DEFAULT_CREATED_AT.toString())));
    }

    @Test
    @Transactional
    void getFeedItem() throws Exception {
        // Initialize the database
        insertedFeedItem = feedItemRepository.saveAndFlush(feedItem);

        // Get the feedItem
        restFeedItemMockMvc
            .perform(get(ENTITY_API_URL_ID, feedItem.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(feedItem.getId().toString()))
            .andExpect(jsonPath("$.userId").value(DEFAULT_USER_ID.toString()))
            .andExpect(jsonPath("$.postId").value(DEFAULT_POST_ID.toString()))
            .andExpect(jsonPath("$.createdAt").value(DEFAULT_CREATED_AT.toString()));
    }

    @Test
    @Transactional
    void getFeedItemsByIdFiltering() throws Exception {
        // Initialize the database
        insertedFeedItem = feedItemRepository.saveAndFlush(feedItem);

        UUID id = feedItem.getId();

        defaultFeedItemFiltering("id.equals=" + id, "id.notEquals=" + id);
    }

    @Test
    @Transactional
    void getAllFeedItemsByUserIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedFeedItem = feedItemRepository.saveAndFlush(feedItem);

        // Get all the feedItemList where userId equals to
        defaultFeedItemFiltering("userId.equals=" + DEFAULT_USER_ID, "userId.equals=" + UPDATED_USER_ID);
    }

    @Test
    @Transactional
    void getAllFeedItemsByUserIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedFeedItem = feedItemRepository.saveAndFlush(feedItem);

        // Get all the feedItemList where userId in
        defaultFeedItemFiltering("userId.in=" + DEFAULT_USER_ID + "," + UPDATED_USER_ID, "userId.in=" + UPDATED_USER_ID);
    }

    @Test
    @Transactional
    void getAllFeedItemsByUserIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedFeedItem = feedItemRepository.saveAndFlush(feedItem);

        // Get all the feedItemList where userId is not null
        defaultFeedItemFiltering("userId.specified=true", "userId.specified=false");
    }

    @Test
    @Transactional
    void getAllFeedItemsByPostIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedFeedItem = feedItemRepository.saveAndFlush(feedItem);

        // Get all the feedItemList where postId equals to
        defaultFeedItemFiltering("postId.equals=" + DEFAULT_POST_ID, "postId.equals=" + UPDATED_POST_ID);
    }

    @Test
    @Transactional
    void getAllFeedItemsByPostIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedFeedItem = feedItemRepository.saveAndFlush(feedItem);

        // Get all the feedItemList where postId in
        defaultFeedItemFiltering("postId.in=" + DEFAULT_POST_ID + "," + UPDATED_POST_ID, "postId.in=" + UPDATED_POST_ID);
    }

    @Test
    @Transactional
    void getAllFeedItemsByPostIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedFeedItem = feedItemRepository.saveAndFlush(feedItem);

        // Get all the feedItemList where postId is not null
        defaultFeedItemFiltering("postId.specified=true", "postId.specified=false");
    }

    @Test
    @Transactional
    void getAllFeedItemsByCreatedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedFeedItem = feedItemRepository.saveAndFlush(feedItem);

        // Get all the feedItemList where createdAt equals to
        defaultFeedItemFiltering("createdAt.equals=" + DEFAULT_CREATED_AT, "createdAt.equals=" + UPDATED_CREATED_AT);
    }

    @Test
    @Transactional
    void getAllFeedItemsByCreatedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedFeedItem = feedItemRepository.saveAndFlush(feedItem);

        // Get all the feedItemList where createdAt in
        defaultFeedItemFiltering("createdAt.in=" + DEFAULT_CREATED_AT + "," + UPDATED_CREATED_AT, "createdAt.in=" + UPDATED_CREATED_AT);
    }

    @Test
    @Transactional
    void getAllFeedItemsByCreatedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedFeedItem = feedItemRepository.saveAndFlush(feedItem);

        // Get all the feedItemList where createdAt is not null
        defaultFeedItemFiltering("createdAt.specified=true", "createdAt.specified=false");
    }

    private void defaultFeedItemFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultFeedItemShouldBeFound(shouldBeFound);
        defaultFeedItemShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultFeedItemShouldBeFound(String filter) throws Exception {
        restFeedItemMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(feedItem.getId().toString())))
            .andExpect(jsonPath("$.[*].userId").value(hasItem(DEFAULT_USER_ID.toString())))
            .andExpect(jsonPath("$.[*].postId").value(hasItem(DEFAULT_POST_ID.toString())))
            .andExpect(jsonPath("$.[*].createdAt").value(hasItem(DEFAULT_CREATED_AT.toString())));

        // Check, that the count call also returns 1
        restFeedItemMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultFeedItemShouldNotBeFound(String filter) throws Exception {
        restFeedItemMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restFeedItemMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingFeedItem() throws Exception {
        // Get the feedItem
        restFeedItemMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingFeedItem() throws Exception {
        // Initialize the database
        insertedFeedItem = feedItemRepository.saveAndFlush(feedItem);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the feedItem
        FeedItem updatedFeedItem = feedItemRepository.findById(feedItem.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedFeedItem are not directly saved in db
        em.detach(updatedFeedItem);
        updatedFeedItem.userId(UPDATED_USER_ID).postId(UPDATED_POST_ID).createdAt(UPDATED_CREATED_AT);
        FeedItemDTO feedItemDTO = feedItemMapper.toDto(updatedFeedItem);

        restFeedItemMockMvc
            .perform(
                put(ENTITY_API_URL_ID, feedItemDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(feedItemDTO))
            )
            .andExpect(status().isOk());

        // Validate the FeedItem in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedFeedItemToMatchAllProperties(updatedFeedItem);
    }

    @Test
    @Transactional
    void putNonExistingFeedItem() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        feedItem.setId(UUID.randomUUID());

        // Create the FeedItem
        FeedItemDTO feedItemDTO = feedItemMapper.toDto(feedItem);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restFeedItemMockMvc
            .perform(
                put(ENTITY_API_URL_ID, feedItemDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(feedItemDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the FeedItem in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchFeedItem() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        feedItem.setId(UUID.randomUUID());

        // Create the FeedItem
        FeedItemDTO feedItemDTO = feedItemMapper.toDto(feedItem);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restFeedItemMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(feedItemDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the FeedItem in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamFeedItem() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        feedItem.setId(UUID.randomUUID());

        // Create the FeedItem
        FeedItemDTO feedItemDTO = feedItemMapper.toDto(feedItem);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restFeedItemMockMvc
            .perform(put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(feedItemDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the FeedItem in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateFeedItemWithPatch() throws Exception {
        // Initialize the database
        insertedFeedItem = feedItemRepository.saveAndFlush(feedItem);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the feedItem using partial update
        FeedItem partialUpdatedFeedItem = new FeedItem();
        partialUpdatedFeedItem.setId(feedItem.getId());

        partialUpdatedFeedItem.postId(UPDATED_POST_ID);

        restFeedItemMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedFeedItem.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedFeedItem))
            )
            .andExpect(status().isOk());

        // Validate the FeedItem in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertFeedItemUpdatableFieldsEquals(createUpdateProxyForBean(partialUpdatedFeedItem, feedItem), getPersistedFeedItem(feedItem));
    }

    @Test
    @Transactional
    void fullUpdateFeedItemWithPatch() throws Exception {
        // Initialize the database
        insertedFeedItem = feedItemRepository.saveAndFlush(feedItem);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the feedItem using partial update
        FeedItem partialUpdatedFeedItem = new FeedItem();
        partialUpdatedFeedItem.setId(feedItem.getId());

        partialUpdatedFeedItem.userId(UPDATED_USER_ID).postId(UPDATED_POST_ID).createdAt(UPDATED_CREATED_AT);

        restFeedItemMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedFeedItem.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedFeedItem))
            )
            .andExpect(status().isOk());

        // Validate the FeedItem in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertFeedItemUpdatableFieldsEquals(partialUpdatedFeedItem, getPersistedFeedItem(partialUpdatedFeedItem));
    }

    @Test
    @Transactional
    void patchNonExistingFeedItem() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        feedItem.setId(UUID.randomUUID());

        // Create the FeedItem
        FeedItemDTO feedItemDTO = feedItemMapper.toDto(feedItem);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restFeedItemMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, feedItemDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(feedItemDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the FeedItem in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchFeedItem() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        feedItem.setId(UUID.randomUUID());

        // Create the FeedItem
        FeedItemDTO feedItemDTO = feedItemMapper.toDto(feedItem);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restFeedItemMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(feedItemDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the FeedItem in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamFeedItem() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        feedItem.setId(UUID.randomUUID());

        // Create the FeedItem
        FeedItemDTO feedItemDTO = feedItemMapper.toDto(feedItem);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restFeedItemMockMvc
            .perform(
                patch(ENTITY_API_URL).with(csrf()).contentType("application/merge-patch+json").content(om.writeValueAsBytes(feedItemDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the FeedItem in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteFeedItem() throws Exception {
        // Initialize the database
        insertedFeedItem = feedItemRepository.saveAndFlush(feedItem);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the feedItem
        restFeedItemMockMvc
            .perform(delete(ENTITY_API_URL_ID, feedItem.getId().toString()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return feedItemRepository.count();
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

    protected FeedItem getPersistedFeedItem(FeedItem feedItem) {
        return feedItemRepository.findById(feedItem.getId()).orElseThrow();
    }

    protected void assertPersistedFeedItemToMatchAllProperties(FeedItem expectedFeedItem) {
        assertFeedItemAllPropertiesEquals(expectedFeedItem, getPersistedFeedItem(expectedFeedItem));
    }

    protected void assertPersistedFeedItemToMatchUpdatableProperties(FeedItem expectedFeedItem) {
        assertFeedItemAllUpdatablePropertiesEquals(expectedFeedItem, getPersistedFeedItem(expectedFeedItem));
    }
}
