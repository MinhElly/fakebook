package com.minh.fakebook.user.web.rest;

import static com.minh.fakebook.user.domain.FriendRequestAsserts.*;
import static com.minh.fakebook.user.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.minh.fakebook.user.IntegrationTest;
import com.minh.fakebook.user.domain.FriendRequest;
import com.minh.fakebook.user.domain.UserProfile;
import com.minh.fakebook.user.domain.enumeration.FriendRequestStatus;
import com.minh.fakebook.user.repository.FriendRequestRepository;
import com.minh.fakebook.user.service.dto.FriendRequestDTO;
import com.minh.fakebook.user.service.mapper.FriendRequestMapper;
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
 * Integration tests for the {@link FriendRequestResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class FriendRequestResourceIT {

    private static final FriendRequestStatus DEFAULT_STATUS = FriendRequestStatus.PENDING;
    private static final FriendRequestStatus UPDATED_STATUS = FriendRequestStatus.ACCEPTED;

    private static final Instant DEFAULT_CREATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_CREATED_AT = Instant.ofEpochMilli(1702182417233L);

    private static final Instant DEFAULT_RESPONDED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_RESPONDED_AT = Instant.ofEpochMilli(1702182417233L);

    private static final String ENTITY_API_URL = "/api/friend-requests";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private FriendRequestRepository friendRequestRepository;

    @Autowired
    private FriendRequestMapper friendRequestMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restFriendRequestMockMvc;

    private FriendRequest friendRequest;

    private FriendRequest insertedFriendRequest;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static FriendRequest createEntity(EntityManager em) {
        FriendRequest friendRequest = new FriendRequest()
            .status(DEFAULT_STATUS)
            .createdAt(DEFAULT_CREATED_AT)
            .respondedAt(DEFAULT_RESPONDED_AT);
        // Add required entity
        UserProfile userProfile;
        if (TestUtil.findAll(em, UserProfile.class).isEmpty()) {
            userProfile = UserProfileResourceIT.createEntity();
            em.persist(userProfile);
            em.flush();
        } else {
            userProfile = TestUtil.findAll(em, UserProfile.class).get(0);
        }
        friendRequest.setSender(userProfile);
        // Add required entity
        friendRequest.setReceiver(userProfile);
        return friendRequest;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static FriendRequest createUpdatedEntity(EntityManager em) {
        FriendRequest updatedFriendRequest = new FriendRequest()
            .status(UPDATED_STATUS)
            .createdAt(UPDATED_CREATED_AT)
            .respondedAt(UPDATED_RESPONDED_AT);
        // Add required entity
        UserProfile userProfile;
        if (TestUtil.findAll(em, UserProfile.class).isEmpty()) {
            userProfile = UserProfileResourceIT.createUpdatedEntity();
            em.persist(userProfile);
            em.flush();
        } else {
            userProfile = TestUtil.findAll(em, UserProfile.class).get(0);
        }
        updatedFriendRequest.setSender(userProfile);
        // Add required entity
        updatedFriendRequest.setReceiver(userProfile);
        return updatedFriendRequest;
    }

    @BeforeEach
    void initTest() {
        friendRequest = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedFriendRequest != null) {
            friendRequestRepository.delete(insertedFriendRequest);
            insertedFriendRequest = null;
        }
    }

    @Test
    @Transactional
    void createFriendRequest() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the FriendRequest
        FriendRequestDTO friendRequestDTO = friendRequestMapper.toDto(friendRequest);
        var returnedFriendRequestDTO = om.readValue(
            restFriendRequestMockMvc
                .perform(
                    post(ENTITY_API_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsBytes(friendRequestDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            FriendRequestDTO.class
        );

        // Validate the FriendRequest in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedFriendRequest = friendRequestMapper.toEntity(returnedFriendRequestDTO);
        assertFriendRequestUpdatableFieldsEquals(returnedFriendRequest, getPersistedFriendRequest(returnedFriendRequest));

        insertedFriendRequest = returnedFriendRequest;
    }

    @Test
    @Transactional
    void createFriendRequestWithExistingId() throws Exception {
        // Create the FriendRequest with an existing ID
        insertedFriendRequest = friendRequestRepository.saveAndFlush(friendRequest);
        FriendRequestDTO friendRequestDTO = friendRequestMapper.toDto(friendRequest);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restFriendRequestMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(friendRequestDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the FriendRequest in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkStatusIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        friendRequest.setStatus(null);

        // Create the FriendRequest, which fails.
        FriendRequestDTO friendRequestDTO = friendRequestMapper.toDto(friendRequest);

        restFriendRequestMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(friendRequestDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkCreatedAtIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        friendRequest.setCreatedAt(null);

        // Create the FriendRequest, which fails.
        FriendRequestDTO friendRequestDTO = friendRequestMapper.toDto(friendRequest);

        restFriendRequestMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(friendRequestDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllFriendRequests() throws Exception {
        // Initialize the database
        insertedFriendRequest = friendRequestRepository.saveAndFlush(friendRequest);

        // Get all the friendRequestList
        restFriendRequestMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(friendRequest.getId().toString())))
            .andExpect(jsonPath("$.[*].status").value(hasItem(DEFAULT_STATUS.toString())))
            .andExpect(jsonPath("$.[*].createdAt").value(hasItem(DEFAULT_CREATED_AT.toString())))
            .andExpect(jsonPath("$.[*].respondedAt").value(hasItem(DEFAULT_RESPONDED_AT.toString())));
    }

    @Test
    @Transactional
    void getFriendRequest() throws Exception {
        // Initialize the database
        insertedFriendRequest = friendRequestRepository.saveAndFlush(friendRequest);

        // Get the friendRequest
        restFriendRequestMockMvc
            .perform(get(ENTITY_API_URL_ID, friendRequest.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(friendRequest.getId().toString()))
            .andExpect(jsonPath("$.status").value(DEFAULT_STATUS.toString()))
            .andExpect(jsonPath("$.createdAt").value(DEFAULT_CREATED_AT.toString()))
            .andExpect(jsonPath("$.respondedAt").value(DEFAULT_RESPONDED_AT.toString()));
    }

    @Test
    @Transactional
    void getFriendRequestsByIdFiltering() throws Exception {
        // Initialize the database
        insertedFriendRequest = friendRequestRepository.saveAndFlush(friendRequest);

        UUID id = friendRequest.getId();

        defaultFriendRequestFiltering("id.equals=" + id, "id.notEquals=" + id);
    }

    @Test
    @Transactional
    void getAllFriendRequestsByStatusIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedFriendRequest = friendRequestRepository.saveAndFlush(friendRequest);

        // Get all the friendRequestList where status equals to
        defaultFriendRequestFiltering("status.equals=" + DEFAULT_STATUS, "status.equals=" + UPDATED_STATUS);
    }

    @Test
    @Transactional
    void getAllFriendRequestsByStatusIsInShouldWork() throws Exception {
        // Initialize the database
        insertedFriendRequest = friendRequestRepository.saveAndFlush(friendRequest);

        // Get all the friendRequestList where status in
        defaultFriendRequestFiltering("status.in=" + DEFAULT_STATUS + "," + UPDATED_STATUS, "status.in=" + UPDATED_STATUS);
    }

    @Test
    @Transactional
    void getAllFriendRequestsByStatusIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedFriendRequest = friendRequestRepository.saveAndFlush(friendRequest);

        // Get all the friendRequestList where status is not null
        defaultFriendRequestFiltering("status.specified=true", "status.specified=false");
    }

    @Test
    @Transactional
    void getAllFriendRequestsByCreatedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedFriendRequest = friendRequestRepository.saveAndFlush(friendRequest);

        // Get all the friendRequestList where createdAt equals to
        defaultFriendRequestFiltering("createdAt.equals=" + DEFAULT_CREATED_AT, "createdAt.equals=" + UPDATED_CREATED_AT);
    }

    @Test
    @Transactional
    void getAllFriendRequestsByCreatedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedFriendRequest = friendRequestRepository.saveAndFlush(friendRequest);

        // Get all the friendRequestList where createdAt in
        defaultFriendRequestFiltering(
            "createdAt.in=" + DEFAULT_CREATED_AT + "," + UPDATED_CREATED_AT,
            "createdAt.in=" + UPDATED_CREATED_AT
        );
    }

    @Test
    @Transactional
    void getAllFriendRequestsByCreatedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedFriendRequest = friendRequestRepository.saveAndFlush(friendRequest);

        // Get all the friendRequestList where createdAt is not null
        defaultFriendRequestFiltering("createdAt.specified=true", "createdAt.specified=false");
    }

    @Test
    @Transactional
    void getAllFriendRequestsByRespondedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedFriendRequest = friendRequestRepository.saveAndFlush(friendRequest);

        // Get all the friendRequestList where respondedAt equals to
        defaultFriendRequestFiltering("respondedAt.equals=" + DEFAULT_RESPONDED_AT, "respondedAt.equals=" + UPDATED_RESPONDED_AT);
    }

    @Test
    @Transactional
    void getAllFriendRequestsByRespondedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedFriendRequest = friendRequestRepository.saveAndFlush(friendRequest);

        // Get all the friendRequestList where respondedAt in
        defaultFriendRequestFiltering(
            "respondedAt.in=" + DEFAULT_RESPONDED_AT + "," + UPDATED_RESPONDED_AT,
            "respondedAt.in=" + UPDATED_RESPONDED_AT
        );
    }

    @Test
    @Transactional
    void getAllFriendRequestsByRespondedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedFriendRequest = friendRequestRepository.saveAndFlush(friendRequest);

        // Get all the friendRequestList where respondedAt is not null
        defaultFriendRequestFiltering("respondedAt.specified=true", "respondedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllFriendRequestsBySenderIsEqualToSomething() throws Exception {
        UserProfile sender;
        if (TestUtil.findAll(em, UserProfile.class).isEmpty()) {
            friendRequestRepository.saveAndFlush(friendRequest);
            sender = UserProfileResourceIT.createEntity();
        } else {
            sender = TestUtil.findAll(em, UserProfile.class).get(0);
        }
        em.persist(sender);
        em.flush();
        friendRequest.setSender(sender);
        friendRequestRepository.saveAndFlush(friendRequest);
        UUID senderId = sender.getId();
        // Get all the friendRequestList where sender equals to senderId
        defaultFriendRequestShouldBeFound("senderId.equals=" + senderId);

        // Get all the friendRequestList where sender equals to UUID.randomUUID()
        defaultFriendRequestShouldNotBeFound("senderId.equals=" + UUID.randomUUID());
    }

    @Test
    @Transactional
    void getAllFriendRequestsByReceiverIsEqualToSomething() throws Exception {
        UserProfile receiver;
        if (TestUtil.findAll(em, UserProfile.class).isEmpty()) {
            friendRequestRepository.saveAndFlush(friendRequest);
            receiver = UserProfileResourceIT.createEntity();
        } else {
            receiver = TestUtil.findAll(em, UserProfile.class).get(0);
        }
        em.persist(receiver);
        em.flush();
        friendRequest.setReceiver(receiver);
        friendRequestRepository.saveAndFlush(friendRequest);
        UUID receiverId = receiver.getId();
        // Get all the friendRequestList where receiver equals to receiverId
        defaultFriendRequestShouldBeFound("receiverId.equals=" + receiverId);

        // Get all the friendRequestList where receiver equals to UUID.randomUUID()
        defaultFriendRequestShouldNotBeFound("receiverId.equals=" + UUID.randomUUID());
    }

    private void defaultFriendRequestFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultFriendRequestShouldBeFound(shouldBeFound);
        defaultFriendRequestShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultFriendRequestShouldBeFound(String filter) throws Exception {
        restFriendRequestMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(friendRequest.getId().toString())))
            .andExpect(jsonPath("$.[*].status").value(hasItem(DEFAULT_STATUS.toString())))
            .andExpect(jsonPath("$.[*].createdAt").value(hasItem(DEFAULT_CREATED_AT.toString())))
            .andExpect(jsonPath("$.[*].respondedAt").value(hasItem(DEFAULT_RESPONDED_AT.toString())));

        // Check, that the count call also returns 1
        restFriendRequestMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultFriendRequestShouldNotBeFound(String filter) throws Exception {
        restFriendRequestMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restFriendRequestMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingFriendRequest() throws Exception {
        // Get the friendRequest
        restFriendRequestMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingFriendRequest() throws Exception {
        // Initialize the database
        insertedFriendRequest = friendRequestRepository.saveAndFlush(friendRequest);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the friendRequest
        FriendRequest updatedFriendRequest = friendRequestRepository.findById(friendRequest.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedFriendRequest are not directly saved in db
        em.detach(updatedFriendRequest);
        updatedFriendRequest.status(UPDATED_STATUS).createdAt(UPDATED_CREATED_AT).respondedAt(UPDATED_RESPONDED_AT);
        FriendRequestDTO friendRequestDTO = friendRequestMapper.toDto(updatedFriendRequest);

        restFriendRequestMockMvc
            .perform(
                put(ENTITY_API_URL_ID, friendRequestDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(friendRequestDTO))
            )
            .andExpect(status().isOk());

        // Validate the FriendRequest in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedFriendRequestToMatchAllProperties(updatedFriendRequest);
    }

    @Test
    @Transactional
    void putNonExistingFriendRequest() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        friendRequest.setId(UUID.randomUUID());

        // Create the FriendRequest
        FriendRequestDTO friendRequestDTO = friendRequestMapper.toDto(friendRequest);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restFriendRequestMockMvc
            .perform(
                put(ENTITY_API_URL_ID, friendRequestDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(friendRequestDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the FriendRequest in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchFriendRequest() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        friendRequest.setId(UUID.randomUUID());

        // Create the FriendRequest
        FriendRequestDTO friendRequestDTO = friendRequestMapper.toDto(friendRequest);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restFriendRequestMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(friendRequestDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the FriendRequest in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamFriendRequest() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        friendRequest.setId(UUID.randomUUID());

        // Create the FriendRequest
        FriendRequestDTO friendRequestDTO = friendRequestMapper.toDto(friendRequest);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restFriendRequestMockMvc
            .perform(
                put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(friendRequestDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the FriendRequest in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateFriendRequestWithPatch() throws Exception {
        // Initialize the database
        insertedFriendRequest = friendRequestRepository.saveAndFlush(friendRequest);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the friendRequest using partial update
        FriendRequest partialUpdatedFriendRequest = new FriendRequest();
        partialUpdatedFriendRequest.setId(friendRequest.getId());

        partialUpdatedFriendRequest.status(UPDATED_STATUS).createdAt(UPDATED_CREATED_AT).respondedAt(UPDATED_RESPONDED_AT);

        restFriendRequestMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedFriendRequest.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedFriendRequest))
            )
            .andExpect(status().isOk());

        // Validate the FriendRequest in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertFriendRequestUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedFriendRequest, friendRequest),
            getPersistedFriendRequest(friendRequest)
        );
    }

    @Test
    @Transactional
    void fullUpdateFriendRequestWithPatch() throws Exception {
        // Initialize the database
        insertedFriendRequest = friendRequestRepository.saveAndFlush(friendRequest);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the friendRequest using partial update
        FriendRequest partialUpdatedFriendRequest = new FriendRequest();
        partialUpdatedFriendRequest.setId(friendRequest.getId());

        partialUpdatedFriendRequest.status(UPDATED_STATUS).createdAt(UPDATED_CREATED_AT).respondedAt(UPDATED_RESPONDED_AT);

        restFriendRequestMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedFriendRequest.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedFriendRequest))
            )
            .andExpect(status().isOk());

        // Validate the FriendRequest in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertFriendRequestUpdatableFieldsEquals(partialUpdatedFriendRequest, getPersistedFriendRequest(partialUpdatedFriendRequest));
    }

    @Test
    @Transactional
    void patchNonExistingFriendRequest() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        friendRequest.setId(UUID.randomUUID());

        // Create the FriendRequest
        FriendRequestDTO friendRequestDTO = friendRequestMapper.toDto(friendRequest);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restFriendRequestMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, friendRequestDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(friendRequestDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the FriendRequest in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchFriendRequest() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        friendRequest.setId(UUID.randomUUID());

        // Create the FriendRequest
        FriendRequestDTO friendRequestDTO = friendRequestMapper.toDto(friendRequest);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restFriendRequestMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(friendRequestDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the FriendRequest in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamFriendRequest() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        friendRequest.setId(UUID.randomUUID());

        // Create the FriendRequest
        FriendRequestDTO friendRequestDTO = friendRequestMapper.toDto(friendRequest);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restFriendRequestMockMvc
            .perform(
                patch(ENTITY_API_URL)
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(friendRequestDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the FriendRequest in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteFriendRequest() throws Exception {
        // Initialize the database
        insertedFriendRequest = friendRequestRepository.saveAndFlush(friendRequest);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the friendRequest
        restFriendRequestMockMvc
            .perform(delete(ENTITY_API_URL_ID, friendRequest.getId().toString()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return friendRequestRepository.count();
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

    protected FriendRequest getPersistedFriendRequest(FriendRequest friendRequest) {
        return friendRequestRepository.findById(friendRequest.getId()).orElseThrow();
    }

    protected void assertPersistedFriendRequestToMatchAllProperties(FriendRequest expectedFriendRequest) {
        assertFriendRequestAllPropertiesEquals(expectedFriendRequest, getPersistedFriendRequest(expectedFriendRequest));
    }

    protected void assertPersistedFriendRequestToMatchUpdatableProperties(FriendRequest expectedFriendRequest) {
        assertFriendRequestAllUpdatablePropertiesEquals(expectedFriendRequest, getPersistedFriendRequest(expectedFriendRequest));
    }
}
