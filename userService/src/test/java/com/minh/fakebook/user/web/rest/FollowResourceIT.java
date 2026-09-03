package com.minh.fakebook.user.web.rest;

import static com.minh.fakebook.user.domain.FollowAsserts.*;
import static com.minh.fakebook.user.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.minh.fakebook.user.IntegrationTest;
import com.minh.fakebook.user.domain.Follow;
import com.minh.fakebook.user.domain.UserProfile;
import com.minh.fakebook.user.repository.FollowRepository;
import com.minh.fakebook.user.service.dto.FollowDTO;
import com.minh.fakebook.user.service.mapper.FollowMapper;
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
 * Integration tests for the {@link FollowResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class FollowResourceIT {

    private static final Instant DEFAULT_CREATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_CREATED_AT = Instant.ofEpochMilli(1702182417233L);

    private static final String ENTITY_API_URL = "/api/follows";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private FollowRepository followRepository;

    @Autowired
    private FollowMapper followMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restFollowMockMvc;

    private Follow follow;

    private Follow insertedFollow;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Follow createEntity(EntityManager em) {
        Follow follow = new Follow().createdAt(DEFAULT_CREATED_AT);
        // Add required entity
        UserProfile userProfile;
        if (TestUtil.findAll(em, UserProfile.class).isEmpty()) {
            userProfile = UserProfileResourceIT.createEntity();
            em.persist(userProfile);
            em.flush();
        } else {
            userProfile = TestUtil.findAll(em, UserProfile.class).get(0);
        }
        follow.setFollower(userProfile);
        // Add required entity
        follow.setFollowing(userProfile);
        return follow;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Follow createUpdatedEntity(EntityManager em) {
        Follow updatedFollow = new Follow().createdAt(UPDATED_CREATED_AT);
        // Add required entity
        UserProfile userProfile;
        if (TestUtil.findAll(em, UserProfile.class).isEmpty()) {
            userProfile = UserProfileResourceIT.createUpdatedEntity();
            em.persist(userProfile);
            em.flush();
        } else {
            userProfile = TestUtil.findAll(em, UserProfile.class).get(0);
        }
        updatedFollow.setFollower(userProfile);
        // Add required entity
        updatedFollow.setFollowing(userProfile);
        return updatedFollow;
    }

    @BeforeEach
    void initTest() {
        follow = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedFollow != null) {
            followRepository.delete(insertedFollow);
            insertedFollow = null;
        }
    }

    @Test
    @Transactional
    void createFollow() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the Follow
        FollowDTO followDTO = followMapper.toDto(follow);
        var returnedFollowDTO = om.readValue(
            restFollowMockMvc
                .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(followDTO)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            FollowDTO.class
        );

        // Validate the Follow in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedFollow = followMapper.toEntity(returnedFollowDTO);
        assertFollowUpdatableFieldsEquals(returnedFollow, getPersistedFollow(returnedFollow));

        insertedFollow = returnedFollow;
    }

    @Test
    @Transactional
    void createFollowWithExistingId() throws Exception {
        // Create the Follow with an existing ID
        insertedFollow = followRepository.saveAndFlush(follow);
        FollowDTO followDTO = followMapper.toDto(follow);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restFollowMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(followDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Follow in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkCreatedAtIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        follow.setCreatedAt(null);

        // Create the Follow, which fails.
        FollowDTO followDTO = followMapper.toDto(follow);

        restFollowMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(followDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllFollows() throws Exception {
        // Initialize the database
        insertedFollow = followRepository.saveAndFlush(follow);

        // Get all the followList
        restFollowMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(follow.getId().toString())))
            .andExpect(jsonPath("$.[*].createdAt").value(hasItem(DEFAULT_CREATED_AT.toString())));
    }

    @Test
    @Transactional
    void getFollow() throws Exception {
        // Initialize the database
        insertedFollow = followRepository.saveAndFlush(follow);

        // Get the follow
        restFollowMockMvc
            .perform(get(ENTITY_API_URL_ID, follow.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(follow.getId().toString()))
            .andExpect(jsonPath("$.createdAt").value(DEFAULT_CREATED_AT.toString()));
    }

    @Test
    @Transactional
    void getFollowsByIdFiltering() throws Exception {
        // Initialize the database
        insertedFollow = followRepository.saveAndFlush(follow);

        UUID id = follow.getId();

        defaultFollowFiltering("id.equals=" + id, "id.notEquals=" + id);
    }

    @Test
    @Transactional
    void getAllFollowsByCreatedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedFollow = followRepository.saveAndFlush(follow);

        // Get all the followList where createdAt equals to
        defaultFollowFiltering("createdAt.equals=" + DEFAULT_CREATED_AT, "createdAt.equals=" + UPDATED_CREATED_AT);
    }

    @Test
    @Transactional
    void getAllFollowsByCreatedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedFollow = followRepository.saveAndFlush(follow);

        // Get all the followList where createdAt in
        defaultFollowFiltering("createdAt.in=" + DEFAULT_CREATED_AT + "," + UPDATED_CREATED_AT, "createdAt.in=" + UPDATED_CREATED_AT);
    }

    @Test
    @Transactional
    void getAllFollowsByCreatedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedFollow = followRepository.saveAndFlush(follow);

        // Get all the followList where createdAt is not null
        defaultFollowFiltering("createdAt.specified=true", "createdAt.specified=false");
    }

    @Test
    @Transactional
    void getAllFollowsByFollowerIsEqualToSomething() throws Exception {
        UserProfile follower;
        if (TestUtil.findAll(em, UserProfile.class).isEmpty()) {
            followRepository.saveAndFlush(follow);
            follower = UserProfileResourceIT.createEntity();
        } else {
            follower = TestUtil.findAll(em, UserProfile.class).get(0);
        }
        em.persist(follower);
        em.flush();
        follow.setFollower(follower);
        followRepository.saveAndFlush(follow);
        UUID followerId = follower.getId();
        // Get all the followList where follower equals to followerId
        defaultFollowShouldBeFound("followerId.equals=" + followerId);

        // Get all the followList where follower equals to UUID.randomUUID()
        defaultFollowShouldNotBeFound("followerId.equals=" + UUID.randomUUID());
    }

    @Test
    @Transactional
    void getAllFollowsByFollowingIsEqualToSomething() throws Exception {
        UserProfile following;
        if (TestUtil.findAll(em, UserProfile.class).isEmpty()) {
            followRepository.saveAndFlush(follow);
            following = UserProfileResourceIT.createEntity();
        } else {
            following = TestUtil.findAll(em, UserProfile.class).get(0);
        }
        em.persist(following);
        em.flush();
        follow.setFollowing(following);
        followRepository.saveAndFlush(follow);
        UUID followingId = following.getId();
        // Get all the followList where following equals to followingId
        defaultFollowShouldBeFound("followingId.equals=" + followingId);

        // Get all the followList where following equals to UUID.randomUUID()
        defaultFollowShouldNotBeFound("followingId.equals=" + UUID.randomUUID());
    }

    private void defaultFollowFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultFollowShouldBeFound(shouldBeFound);
        defaultFollowShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultFollowShouldBeFound(String filter) throws Exception {
        restFollowMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(follow.getId().toString())))
            .andExpect(jsonPath("$.[*].createdAt").value(hasItem(DEFAULT_CREATED_AT.toString())));

        // Check, that the count call also returns 1
        restFollowMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultFollowShouldNotBeFound(String filter) throws Exception {
        restFollowMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restFollowMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingFollow() throws Exception {
        // Get the follow
        restFollowMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingFollow() throws Exception {
        // Initialize the database
        insertedFollow = followRepository.saveAndFlush(follow);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the follow
        Follow updatedFollow = followRepository.findById(follow.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedFollow are not directly saved in db
        em.detach(updatedFollow);
        updatedFollow.createdAt(UPDATED_CREATED_AT);
        FollowDTO followDTO = followMapper.toDto(updatedFollow);

        restFollowMockMvc
            .perform(
                put(ENTITY_API_URL_ID, followDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(followDTO))
            )
            .andExpect(status().isOk());

        // Validate the Follow in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedFollowToMatchAllProperties(updatedFollow);
    }

    @Test
    @Transactional
    void putNonExistingFollow() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        follow.setId(UUID.randomUUID());

        // Create the Follow
        FollowDTO followDTO = followMapper.toDto(follow);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restFollowMockMvc
            .perform(
                put(ENTITY_API_URL_ID, followDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(followDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Follow in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchFollow() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        follow.setId(UUID.randomUUID());

        // Create the Follow
        FollowDTO followDTO = followMapper.toDto(follow);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restFollowMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(followDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Follow in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamFollow() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        follow.setId(UUID.randomUUID());

        // Create the Follow
        FollowDTO followDTO = followMapper.toDto(follow);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restFollowMockMvc
            .perform(put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(followDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Follow in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateFollowWithPatch() throws Exception {
        // Initialize the database
        insertedFollow = followRepository.saveAndFlush(follow);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the follow using partial update
        Follow partialUpdatedFollow = new Follow();
        partialUpdatedFollow.setId(follow.getId());

        partialUpdatedFollow.createdAt(UPDATED_CREATED_AT);

        restFollowMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedFollow.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedFollow))
            )
            .andExpect(status().isOk());

        // Validate the Follow in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertFollowUpdatableFieldsEquals(createUpdateProxyForBean(partialUpdatedFollow, follow), getPersistedFollow(follow));
    }

    @Test
    @Transactional
    void fullUpdateFollowWithPatch() throws Exception {
        // Initialize the database
        insertedFollow = followRepository.saveAndFlush(follow);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the follow using partial update
        Follow partialUpdatedFollow = new Follow();
        partialUpdatedFollow.setId(follow.getId());

        partialUpdatedFollow.createdAt(UPDATED_CREATED_AT);

        restFollowMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedFollow.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedFollow))
            )
            .andExpect(status().isOk());

        // Validate the Follow in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertFollowUpdatableFieldsEquals(partialUpdatedFollow, getPersistedFollow(partialUpdatedFollow));
    }

    @Test
    @Transactional
    void patchNonExistingFollow() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        follow.setId(UUID.randomUUID());

        // Create the Follow
        FollowDTO followDTO = followMapper.toDto(follow);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restFollowMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, followDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(followDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Follow in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchFollow() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        follow.setId(UUID.randomUUID());

        // Create the Follow
        FollowDTO followDTO = followMapper.toDto(follow);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restFollowMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(followDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Follow in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamFollow() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        follow.setId(UUID.randomUUID());

        // Create the Follow
        FollowDTO followDTO = followMapper.toDto(follow);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restFollowMockMvc
            .perform(
                patch(ENTITY_API_URL).with(csrf()).contentType("application/merge-patch+json").content(om.writeValueAsBytes(followDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the Follow in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteFollow() throws Exception {
        // Initialize the database
        insertedFollow = followRepository.saveAndFlush(follow);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the follow
        restFollowMockMvc
            .perform(delete(ENTITY_API_URL_ID, follow.getId().toString()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return followRepository.count();
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

    protected Follow getPersistedFollow(Follow follow) {
        return followRepository.findById(follow.getId()).orElseThrow();
    }

    protected void assertPersistedFollowToMatchAllProperties(Follow expectedFollow) {
        assertFollowAllPropertiesEquals(expectedFollow, getPersistedFollow(expectedFollow));
    }

    protected void assertPersistedFollowToMatchUpdatableProperties(Follow expectedFollow) {
        assertFollowAllUpdatablePropertiesEquals(expectedFollow, getPersistedFollow(expectedFollow));
    }
}
