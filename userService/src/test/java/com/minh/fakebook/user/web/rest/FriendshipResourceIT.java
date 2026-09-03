package com.minh.fakebook.user.web.rest;

import static com.minh.fakebook.user.domain.FriendshipAsserts.*;
import static com.minh.fakebook.user.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.minh.fakebook.user.IntegrationTest;
import com.minh.fakebook.user.domain.Friendship;
import com.minh.fakebook.user.domain.UserProfile;
import com.minh.fakebook.user.repository.FriendshipRepository;
import com.minh.fakebook.user.service.dto.FriendshipDTO;
import com.minh.fakebook.user.service.mapper.FriendshipMapper;
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
 * Integration tests for the {@link FriendshipResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class FriendshipResourceIT {

    private static final Instant DEFAULT_CREATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_CREATED_AT = Instant.ofEpochMilli(1702182417233L);

    private static final String ENTITY_API_URL = "/api/friendships";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private FriendshipRepository friendshipRepository;

    @Autowired
    private FriendshipMapper friendshipMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restFriendshipMockMvc;

    private Friendship friendship;

    private Friendship insertedFriendship;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Friendship createEntity(EntityManager em) {
        Friendship friendship = new Friendship().createdAt(DEFAULT_CREATED_AT);
        // Add required entity
        UserProfile userProfile;
        if (TestUtil.findAll(em, UserProfile.class).isEmpty()) {
            userProfile = UserProfileResourceIT.createEntity();
            em.persist(userProfile);
            em.flush();
        } else {
            userProfile = TestUtil.findAll(em, UserProfile.class).get(0);
        }
        friendship.setUser(userProfile);
        // Add required entity
        friendship.setFriend(userProfile);
        return friendship;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Friendship createUpdatedEntity(EntityManager em) {
        Friendship updatedFriendship = new Friendship().createdAt(UPDATED_CREATED_AT);
        // Add required entity
        UserProfile userProfile;
        if (TestUtil.findAll(em, UserProfile.class).isEmpty()) {
            userProfile = UserProfileResourceIT.createUpdatedEntity();
            em.persist(userProfile);
            em.flush();
        } else {
            userProfile = TestUtil.findAll(em, UserProfile.class).get(0);
        }
        updatedFriendship.setUser(userProfile);
        // Add required entity
        updatedFriendship.setFriend(userProfile);
        return updatedFriendship;
    }

    @BeforeEach
    void initTest() {
        friendship = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedFriendship != null) {
            friendshipRepository.delete(insertedFriendship);
            insertedFriendship = null;
        }
    }

    @Test
    @Transactional
    void createFriendship() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the Friendship
        FriendshipDTO friendshipDTO = friendshipMapper.toDto(friendship);
        var returnedFriendshipDTO = om.readValue(
            restFriendshipMockMvc
                .perform(
                    post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(friendshipDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            FriendshipDTO.class
        );

        // Validate the Friendship in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedFriendship = friendshipMapper.toEntity(returnedFriendshipDTO);
        assertFriendshipUpdatableFieldsEquals(returnedFriendship, getPersistedFriendship(returnedFriendship));

        insertedFriendship = returnedFriendship;
    }

    @Test
    @Transactional
    void createFriendshipWithExistingId() throws Exception {
        // Create the Friendship with an existing ID
        insertedFriendship = friendshipRepository.saveAndFlush(friendship);
        FriendshipDTO friendshipDTO = friendshipMapper.toDto(friendship);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restFriendshipMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(friendshipDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Friendship in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkCreatedAtIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        friendship.setCreatedAt(null);

        // Create the Friendship, which fails.
        FriendshipDTO friendshipDTO = friendshipMapper.toDto(friendship);

        restFriendshipMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(friendshipDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllFriendships() throws Exception {
        // Initialize the database
        insertedFriendship = friendshipRepository.saveAndFlush(friendship);

        // Get all the friendshipList
        restFriendshipMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(friendship.getId().toString())))
            .andExpect(jsonPath("$.[*].createdAt").value(hasItem(DEFAULT_CREATED_AT.toString())));
    }

    @Test
    @Transactional
    void getFriendship() throws Exception {
        // Initialize the database
        insertedFriendship = friendshipRepository.saveAndFlush(friendship);

        // Get the friendship
        restFriendshipMockMvc
            .perform(get(ENTITY_API_URL_ID, friendship.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(friendship.getId().toString()))
            .andExpect(jsonPath("$.createdAt").value(DEFAULT_CREATED_AT.toString()));
    }

    @Test
    @Transactional
    void getFriendshipsByIdFiltering() throws Exception {
        // Initialize the database
        insertedFriendship = friendshipRepository.saveAndFlush(friendship);

        UUID id = friendship.getId();

        defaultFriendshipFiltering("id.equals=" + id, "id.notEquals=" + id);
    }

    @Test
    @Transactional
    void getAllFriendshipsByCreatedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedFriendship = friendshipRepository.saveAndFlush(friendship);

        // Get all the friendshipList where createdAt equals to
        defaultFriendshipFiltering("createdAt.equals=" + DEFAULT_CREATED_AT, "createdAt.equals=" + UPDATED_CREATED_AT);
    }

    @Test
    @Transactional
    void getAllFriendshipsByCreatedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedFriendship = friendshipRepository.saveAndFlush(friendship);

        // Get all the friendshipList where createdAt in
        defaultFriendshipFiltering("createdAt.in=" + DEFAULT_CREATED_AT + "," + UPDATED_CREATED_AT, "createdAt.in=" + UPDATED_CREATED_AT);
    }

    @Test
    @Transactional
    void getAllFriendshipsByCreatedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedFriendship = friendshipRepository.saveAndFlush(friendship);

        // Get all the friendshipList where createdAt is not null
        defaultFriendshipFiltering("createdAt.specified=true", "createdAt.specified=false");
    }

    @Test
    @Transactional
    void getAllFriendshipsByUserIsEqualToSomething() throws Exception {
        UserProfile user;
        if (TestUtil.findAll(em, UserProfile.class).isEmpty()) {
            friendshipRepository.saveAndFlush(friendship);
            user = UserProfileResourceIT.createEntity();
        } else {
            user = TestUtil.findAll(em, UserProfile.class).get(0);
        }
        em.persist(user);
        em.flush();
        friendship.setUser(user);
        friendshipRepository.saveAndFlush(friendship);
        UUID userId = user.getId();
        // Get all the friendshipList where user equals to userId
        defaultFriendshipShouldBeFound("userId.equals=" + userId);

        // Get all the friendshipList where user equals to UUID.randomUUID()
        defaultFriendshipShouldNotBeFound("userId.equals=" + UUID.randomUUID());
    }

    @Test
    @Transactional
    void getAllFriendshipsByFriendIsEqualToSomething() throws Exception {
        UserProfile friend;
        if (TestUtil.findAll(em, UserProfile.class).isEmpty()) {
            friendshipRepository.saveAndFlush(friendship);
            friend = UserProfileResourceIT.createEntity();
        } else {
            friend = TestUtil.findAll(em, UserProfile.class).get(0);
        }
        em.persist(friend);
        em.flush();
        friendship.setFriend(friend);
        friendshipRepository.saveAndFlush(friendship);
        UUID friendId = friend.getId();
        // Get all the friendshipList where friend equals to friendId
        defaultFriendshipShouldBeFound("friendId.equals=" + friendId);

        // Get all the friendshipList where friend equals to UUID.randomUUID()
        defaultFriendshipShouldNotBeFound("friendId.equals=" + UUID.randomUUID());
    }

    private void defaultFriendshipFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultFriendshipShouldBeFound(shouldBeFound);
        defaultFriendshipShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultFriendshipShouldBeFound(String filter) throws Exception {
        restFriendshipMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(friendship.getId().toString())))
            .andExpect(jsonPath("$.[*].createdAt").value(hasItem(DEFAULT_CREATED_AT.toString())));

        // Check, that the count call also returns 1
        restFriendshipMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultFriendshipShouldNotBeFound(String filter) throws Exception {
        restFriendshipMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restFriendshipMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingFriendship() throws Exception {
        // Get the friendship
        restFriendshipMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingFriendship() throws Exception {
        // Initialize the database
        insertedFriendship = friendshipRepository.saveAndFlush(friendship);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the friendship
        Friendship updatedFriendship = friendshipRepository.findById(friendship.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedFriendship are not directly saved in db
        em.detach(updatedFriendship);
        updatedFriendship.createdAt(UPDATED_CREATED_AT);
        FriendshipDTO friendshipDTO = friendshipMapper.toDto(updatedFriendship);

        restFriendshipMockMvc
            .perform(
                put(ENTITY_API_URL_ID, friendshipDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(friendshipDTO))
            )
            .andExpect(status().isOk());

        // Validate the Friendship in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedFriendshipToMatchAllProperties(updatedFriendship);
    }

    @Test
    @Transactional
    void putNonExistingFriendship() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        friendship.setId(UUID.randomUUID());

        // Create the Friendship
        FriendshipDTO friendshipDTO = friendshipMapper.toDto(friendship);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restFriendshipMockMvc
            .perform(
                put(ENTITY_API_URL_ID, friendshipDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(friendshipDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Friendship in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchFriendship() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        friendship.setId(UUID.randomUUID());

        // Create the Friendship
        FriendshipDTO friendshipDTO = friendshipMapper.toDto(friendship);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restFriendshipMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(friendshipDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Friendship in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamFriendship() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        friendship.setId(UUID.randomUUID());

        // Create the Friendship
        FriendshipDTO friendshipDTO = friendshipMapper.toDto(friendship);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restFriendshipMockMvc
            .perform(put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(friendshipDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Friendship in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateFriendshipWithPatch() throws Exception {
        // Initialize the database
        insertedFriendship = friendshipRepository.saveAndFlush(friendship);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the friendship using partial update
        Friendship partialUpdatedFriendship = new Friendship();
        partialUpdatedFriendship.setId(friendship.getId());

        restFriendshipMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedFriendship.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedFriendship))
            )
            .andExpect(status().isOk());

        // Validate the Friendship in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertFriendshipUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedFriendship, friendship),
            getPersistedFriendship(friendship)
        );
    }

    @Test
    @Transactional
    void fullUpdateFriendshipWithPatch() throws Exception {
        // Initialize the database
        insertedFriendship = friendshipRepository.saveAndFlush(friendship);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the friendship using partial update
        Friendship partialUpdatedFriendship = new Friendship();
        partialUpdatedFriendship.setId(friendship.getId());

        partialUpdatedFriendship.createdAt(UPDATED_CREATED_AT);

        restFriendshipMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedFriendship.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedFriendship))
            )
            .andExpect(status().isOk());

        // Validate the Friendship in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertFriendshipUpdatableFieldsEquals(partialUpdatedFriendship, getPersistedFriendship(partialUpdatedFriendship));
    }

    @Test
    @Transactional
    void patchNonExistingFriendship() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        friendship.setId(UUID.randomUUID());

        // Create the Friendship
        FriendshipDTO friendshipDTO = friendshipMapper.toDto(friendship);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restFriendshipMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, friendshipDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(friendshipDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Friendship in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchFriendship() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        friendship.setId(UUID.randomUUID());

        // Create the Friendship
        FriendshipDTO friendshipDTO = friendshipMapper.toDto(friendship);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restFriendshipMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(friendshipDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Friendship in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamFriendship() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        friendship.setId(UUID.randomUUID());

        // Create the Friendship
        FriendshipDTO friendshipDTO = friendshipMapper.toDto(friendship);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restFriendshipMockMvc
            .perform(
                patch(ENTITY_API_URL).with(csrf()).contentType("application/merge-patch+json").content(om.writeValueAsBytes(friendshipDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the Friendship in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteFriendship() throws Exception {
        // Initialize the database
        insertedFriendship = friendshipRepository.saveAndFlush(friendship);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the friendship
        restFriendshipMockMvc
            .perform(delete(ENTITY_API_URL_ID, friendship.getId().toString()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return friendshipRepository.count();
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

    protected Friendship getPersistedFriendship(Friendship friendship) {
        return friendshipRepository.findById(friendship.getId()).orElseThrow();
    }

    protected void assertPersistedFriendshipToMatchAllProperties(Friendship expectedFriendship) {
        assertFriendshipAllPropertiesEquals(expectedFriendship, getPersistedFriendship(expectedFriendship));
    }

    protected void assertPersistedFriendshipToMatchUpdatableProperties(Friendship expectedFriendship) {
        assertFriendshipAllUpdatablePropertiesEquals(expectedFriendship, getPersistedFriendship(expectedFriendship));
    }
}
