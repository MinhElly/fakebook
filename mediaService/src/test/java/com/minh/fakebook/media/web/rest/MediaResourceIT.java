package com.minh.fakebook.media.web.rest;

import static com.minh.fakebook.media.domain.MediaAsserts.*;
import static com.minh.fakebook.media.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.minh.fakebook.media.IntegrationTest;
import com.minh.fakebook.media.domain.Media;
import com.minh.fakebook.media.domain.enumeration.MediaStatus;
import com.minh.fakebook.media.domain.enumeration.MediaType;
import com.minh.fakebook.media.domain.enumeration.StorageProvider;
import com.minh.fakebook.media.repository.MediaRepository;
import com.minh.fakebook.media.service.dto.MediaDTO;
import com.minh.fakebook.media.service.mapper.MediaMapper;
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
 * Integration tests for the {@link MediaResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class MediaResourceIT {

    private static final UUID DEFAULT_OWNER_ID = UUID.randomUUID();
    private static final UUID UPDATED_OWNER_ID = UUID.randomUUID();

    private static final String DEFAULT_FILE_NAME = "AAAAAAAAAA";
    private static final String UPDATED_FILE_NAME = "BBBBBBBBBB";

    private static final MediaType DEFAULT_MEDIA_TYPE = MediaType.IMAGE;
    private static final MediaType UPDATED_MEDIA_TYPE = MediaType.VIDEO;

    private static final String DEFAULT_MIME_TYPE = "AAAAAAAAAA";
    private static final String UPDATED_MIME_TYPE = "BBBBBBBBBB";

    private static final Long DEFAULT_FILE_SIZE = 0L;
    private static final Long UPDATED_FILE_SIZE = 1L;
    private static final Long SMALLER_FILE_SIZE = 0L - 1L;

    private static final StorageProvider DEFAULT_STORAGE_PROVIDER = StorageProvider.CLOUDINARY;
    private static final StorageProvider UPDATED_STORAGE_PROVIDER = StorageProvider.AWS_S3;

    private static final String DEFAULT_STORAGE_KEY = "AAAAAAAAAA";
    private static final String UPDATED_STORAGE_KEY = "BBBBBBBBBB";

    private static final String DEFAULT_URL = "AAAAAAAAAA";
    private static final String UPDATED_URL = "BBBBBBBBBB";

    private static final MediaStatus DEFAULT_STATUS = MediaStatus.ACTIVE;
    private static final MediaStatus UPDATED_STATUS = MediaStatus.DELETED;

    private static final Instant DEFAULT_CREATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_CREATED_AT = Instant.ofEpochMilli(1701856255192L);

    private static final Instant DEFAULT_UPDATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_UPDATED_AT = Instant.ofEpochMilli(1701856255192L);

    private static final String ENTITY_API_URL = "/api/media";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private MediaRepository mediaRepository;

    @Autowired
    private MediaMapper mediaMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restMediaMockMvc;

    private Media media;

    private Media insertedMedia;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Media createEntity() {
        return new Media()
            .ownerId(DEFAULT_OWNER_ID)
            .fileName(DEFAULT_FILE_NAME)
            .mediaType(DEFAULT_MEDIA_TYPE)
            .mimeType(DEFAULT_MIME_TYPE)
            .fileSize(DEFAULT_FILE_SIZE)
            .storageProvider(DEFAULT_STORAGE_PROVIDER)
            .storageKey(DEFAULT_STORAGE_KEY)
            .url(DEFAULT_URL)
            .status(DEFAULT_STATUS)
            .createdAt(DEFAULT_CREATED_AT)
            .updatedAt(DEFAULT_UPDATED_AT);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Media createUpdatedEntity() {
        return new Media()
            .ownerId(UPDATED_OWNER_ID)
            .fileName(UPDATED_FILE_NAME)
            .mediaType(UPDATED_MEDIA_TYPE)
            .mimeType(UPDATED_MIME_TYPE)
            .fileSize(UPDATED_FILE_SIZE)
            .storageProvider(UPDATED_STORAGE_PROVIDER)
            .storageKey(UPDATED_STORAGE_KEY)
            .url(UPDATED_URL)
            .status(UPDATED_STATUS)
            .createdAt(UPDATED_CREATED_AT)
            .updatedAt(UPDATED_UPDATED_AT);
    }

    @BeforeEach
    void initTest() {
        media = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedMedia != null) {
            mediaRepository.delete(insertedMedia);
            insertedMedia = null;
        }
    }

    @Test
    @Transactional
    void createMedia() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the Media
        MediaDTO mediaDTO = mediaMapper.toDto(media);
        var returnedMediaDTO = om.readValue(
            restMediaMockMvc
                .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(mediaDTO)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            MediaDTO.class
        );

        // Validate the Media in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedMedia = mediaMapper.toEntity(returnedMediaDTO);
        assertMediaUpdatableFieldsEquals(returnedMedia, getPersistedMedia(returnedMedia));

        insertedMedia = returnedMedia;
    }

    @Test
    @Transactional
    void createMediaWithExistingId() throws Exception {
        // Create the Media with an existing ID
        insertedMedia = mediaRepository.saveAndFlush(media);
        MediaDTO mediaDTO = mediaMapper.toDto(media);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restMediaMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(mediaDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Media in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkOwnerIdIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        media.setOwnerId(null);

        // Create the Media, which fails.
        MediaDTO mediaDTO = mediaMapper.toDto(media);

        restMediaMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(mediaDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkFileNameIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        media.setFileName(null);

        // Create the Media, which fails.
        MediaDTO mediaDTO = mediaMapper.toDto(media);

        restMediaMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(mediaDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkMediaTypeIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        media.setMediaType(null);

        // Create the Media, which fails.
        MediaDTO mediaDTO = mediaMapper.toDto(media);

        restMediaMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(mediaDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkMimeTypeIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        media.setMimeType(null);

        // Create the Media, which fails.
        MediaDTO mediaDTO = mediaMapper.toDto(media);

        restMediaMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(mediaDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkFileSizeIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        media.setFileSize(null);

        // Create the Media, which fails.
        MediaDTO mediaDTO = mediaMapper.toDto(media);

        restMediaMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(mediaDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkStorageProviderIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        media.setStorageProvider(null);

        // Create the Media, which fails.
        MediaDTO mediaDTO = mediaMapper.toDto(media);

        restMediaMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(mediaDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkStorageKeyIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        media.setStorageKey(null);

        // Create the Media, which fails.
        MediaDTO mediaDTO = mediaMapper.toDto(media);

        restMediaMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(mediaDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkUrlIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        media.setUrl(null);

        // Create the Media, which fails.
        MediaDTO mediaDTO = mediaMapper.toDto(media);

        restMediaMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(mediaDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkStatusIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        media.setStatus(null);

        // Create the Media, which fails.
        MediaDTO mediaDTO = mediaMapper.toDto(media);

        restMediaMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(mediaDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkCreatedAtIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        media.setCreatedAt(null);

        // Create the Media, which fails.
        MediaDTO mediaDTO = mediaMapper.toDto(media);

        restMediaMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(mediaDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllMedias() throws Exception {
        // Initialize the database
        insertedMedia = mediaRepository.saveAndFlush(media);

        // Get all the mediaList
        restMediaMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(media.getId().toString())))
            .andExpect(jsonPath("$.[*].ownerId").value(hasItem(DEFAULT_OWNER_ID.toString())))
            .andExpect(jsonPath("$.[*].fileName").value(hasItem(DEFAULT_FILE_NAME)))
            .andExpect(jsonPath("$.[*].mediaType").value(hasItem(DEFAULT_MEDIA_TYPE.toString())))
            .andExpect(jsonPath("$.[*].mimeType").value(hasItem(DEFAULT_MIME_TYPE)))
            .andExpect(jsonPath("$.[*].fileSize").value(hasItem(DEFAULT_FILE_SIZE.intValue())))
            .andExpect(jsonPath("$.[*].storageProvider").value(hasItem(DEFAULT_STORAGE_PROVIDER.toString())))
            .andExpect(jsonPath("$.[*].storageKey").value(hasItem(DEFAULT_STORAGE_KEY)))
            .andExpect(jsonPath("$.[*].url").value(hasItem(DEFAULT_URL)))
            .andExpect(jsonPath("$.[*].status").value(hasItem(DEFAULT_STATUS.toString())))
            .andExpect(jsonPath("$.[*].createdAt").value(hasItem(DEFAULT_CREATED_AT.toString())))
            .andExpect(jsonPath("$.[*].updatedAt").value(hasItem(DEFAULT_UPDATED_AT.toString())));
    }

    @Test
    @Transactional
    void getMedia() throws Exception {
        // Initialize the database
        insertedMedia = mediaRepository.saveAndFlush(media);

        // Get the media
        restMediaMockMvc
            .perform(get(ENTITY_API_URL_ID, media.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(media.getId().toString()))
            .andExpect(jsonPath("$.ownerId").value(DEFAULT_OWNER_ID.toString()))
            .andExpect(jsonPath("$.fileName").value(DEFAULT_FILE_NAME))
            .andExpect(jsonPath("$.mediaType").value(DEFAULT_MEDIA_TYPE.toString()))
            .andExpect(jsonPath("$.mimeType").value(DEFAULT_MIME_TYPE))
            .andExpect(jsonPath("$.fileSize").value(DEFAULT_FILE_SIZE.intValue()))
            .andExpect(jsonPath("$.storageProvider").value(DEFAULT_STORAGE_PROVIDER.toString()))
            .andExpect(jsonPath("$.storageKey").value(DEFAULT_STORAGE_KEY))
            .andExpect(jsonPath("$.url").value(DEFAULT_URL))
            .andExpect(jsonPath("$.status").value(DEFAULT_STATUS.toString()))
            .andExpect(jsonPath("$.createdAt").value(DEFAULT_CREATED_AT.toString()))
            .andExpect(jsonPath("$.updatedAt").value(DEFAULT_UPDATED_AT.toString()));
    }

    @Test
    @Transactional
    void getMediasByIdFiltering() throws Exception {
        // Initialize the database
        insertedMedia = mediaRepository.saveAndFlush(media);

        UUID id = media.getId();

        defaultMediaFiltering("id.equals=" + id, "id.notEquals=" + id);
    }

    @Test
    @Transactional
    void getAllMediasByOwnerIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedMedia = mediaRepository.saveAndFlush(media);

        // Get all the mediaList where ownerId equals to
        defaultMediaFiltering("ownerId.equals=" + DEFAULT_OWNER_ID, "ownerId.equals=" + UPDATED_OWNER_ID);
    }

    @Test
    @Transactional
    void getAllMediasByOwnerIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedMedia = mediaRepository.saveAndFlush(media);

        // Get all the mediaList where ownerId in
        defaultMediaFiltering("ownerId.in=" + DEFAULT_OWNER_ID + "," + UPDATED_OWNER_ID, "ownerId.in=" + UPDATED_OWNER_ID);
    }

    @Test
    @Transactional
    void getAllMediasByOwnerIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedMedia = mediaRepository.saveAndFlush(media);

        // Get all the mediaList where ownerId is not null
        defaultMediaFiltering("ownerId.specified=true", "ownerId.specified=false");
    }

    @Test
    @Transactional
    void getAllMediasByFileNameIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedMedia = mediaRepository.saveAndFlush(media);

        // Get all the mediaList where fileName equals to
        defaultMediaFiltering("fileName.equals=" + DEFAULT_FILE_NAME, "fileName.equals=" + UPDATED_FILE_NAME);
    }

    @Test
    @Transactional
    void getAllMediasByFileNameIsInShouldWork() throws Exception {
        // Initialize the database
        insertedMedia = mediaRepository.saveAndFlush(media);

        // Get all the mediaList where fileName in
        defaultMediaFiltering("fileName.in=" + DEFAULT_FILE_NAME + "," + UPDATED_FILE_NAME, "fileName.in=" + UPDATED_FILE_NAME);
    }

    @Test
    @Transactional
    void getAllMediasByFileNameIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedMedia = mediaRepository.saveAndFlush(media);

        // Get all the mediaList where fileName is not null
        defaultMediaFiltering("fileName.specified=true", "fileName.specified=false");
    }

    @Test
    @Transactional
    void getAllMediasByFileNameContainsSomething() throws Exception {
        // Initialize the database
        insertedMedia = mediaRepository.saveAndFlush(media);

        // Get all the mediaList where fileName contains
        defaultMediaFiltering("fileName.contains=" + DEFAULT_FILE_NAME, "fileName.contains=" + UPDATED_FILE_NAME);
    }

    @Test
    @Transactional
    void getAllMediasByFileNameNotContainsSomething() throws Exception {
        // Initialize the database
        insertedMedia = mediaRepository.saveAndFlush(media);

        // Get all the mediaList where fileName does not contain
        defaultMediaFiltering("fileName.doesNotContain=" + UPDATED_FILE_NAME, "fileName.doesNotContain=" + DEFAULT_FILE_NAME);
    }

    @Test
    @Transactional
    void getAllMediasByMediaTypeIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedMedia = mediaRepository.saveAndFlush(media);

        // Get all the mediaList where mediaType equals to
        defaultMediaFiltering("mediaType.equals=" + DEFAULT_MEDIA_TYPE, "mediaType.equals=" + UPDATED_MEDIA_TYPE);
    }

    @Test
    @Transactional
    void getAllMediasByMediaTypeIsInShouldWork() throws Exception {
        // Initialize the database
        insertedMedia = mediaRepository.saveAndFlush(media);

        // Get all the mediaList where mediaType in
        defaultMediaFiltering("mediaType.in=" + DEFAULT_MEDIA_TYPE + "," + UPDATED_MEDIA_TYPE, "mediaType.in=" + UPDATED_MEDIA_TYPE);
    }

    @Test
    @Transactional
    void getAllMediasByMediaTypeIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedMedia = mediaRepository.saveAndFlush(media);

        // Get all the mediaList where mediaType is not null
        defaultMediaFiltering("mediaType.specified=true", "mediaType.specified=false");
    }

    @Test
    @Transactional
    void getAllMediasByMimeTypeIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedMedia = mediaRepository.saveAndFlush(media);

        // Get all the mediaList where mimeType equals to
        defaultMediaFiltering("mimeType.equals=" + DEFAULT_MIME_TYPE, "mimeType.equals=" + UPDATED_MIME_TYPE);
    }

    @Test
    @Transactional
    void getAllMediasByMimeTypeIsInShouldWork() throws Exception {
        // Initialize the database
        insertedMedia = mediaRepository.saveAndFlush(media);

        // Get all the mediaList where mimeType in
        defaultMediaFiltering("mimeType.in=" + DEFAULT_MIME_TYPE + "," + UPDATED_MIME_TYPE, "mimeType.in=" + UPDATED_MIME_TYPE);
    }

    @Test
    @Transactional
    void getAllMediasByMimeTypeIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedMedia = mediaRepository.saveAndFlush(media);

        // Get all the mediaList where mimeType is not null
        defaultMediaFiltering("mimeType.specified=true", "mimeType.specified=false");
    }

    @Test
    @Transactional
    void getAllMediasByMimeTypeContainsSomething() throws Exception {
        // Initialize the database
        insertedMedia = mediaRepository.saveAndFlush(media);

        // Get all the mediaList where mimeType contains
        defaultMediaFiltering("mimeType.contains=" + DEFAULT_MIME_TYPE, "mimeType.contains=" + UPDATED_MIME_TYPE);
    }

    @Test
    @Transactional
    void getAllMediasByMimeTypeNotContainsSomething() throws Exception {
        // Initialize the database
        insertedMedia = mediaRepository.saveAndFlush(media);

        // Get all the mediaList where mimeType does not contain
        defaultMediaFiltering("mimeType.doesNotContain=" + UPDATED_MIME_TYPE, "mimeType.doesNotContain=" + DEFAULT_MIME_TYPE);
    }

    @Test
    @Transactional
    void getAllMediasByFileSizeIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedMedia = mediaRepository.saveAndFlush(media);

        // Get all the mediaList where fileSize equals to
        defaultMediaFiltering("fileSize.equals=" + DEFAULT_FILE_SIZE, "fileSize.equals=" + UPDATED_FILE_SIZE);
    }

    @Test
    @Transactional
    void getAllMediasByFileSizeIsInShouldWork() throws Exception {
        // Initialize the database
        insertedMedia = mediaRepository.saveAndFlush(media);

        // Get all the mediaList where fileSize in
        defaultMediaFiltering("fileSize.in=" + DEFAULT_FILE_SIZE + "," + UPDATED_FILE_SIZE, "fileSize.in=" + UPDATED_FILE_SIZE);
    }

    @Test
    @Transactional
    void getAllMediasByFileSizeIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedMedia = mediaRepository.saveAndFlush(media);

        // Get all the mediaList where fileSize is not null
        defaultMediaFiltering("fileSize.specified=true", "fileSize.specified=false");
    }

    @Test
    @Transactional
    void getAllMediasByFileSizeIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedMedia = mediaRepository.saveAndFlush(media);

        // Get all the mediaList where fileSize is greater than or equal to
        defaultMediaFiltering("fileSize.greaterThanOrEqual=" + DEFAULT_FILE_SIZE, "fileSize.greaterThanOrEqual=" + UPDATED_FILE_SIZE);
    }

    @Test
    @Transactional
    void getAllMediasByFileSizeIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedMedia = mediaRepository.saveAndFlush(media);

        // Get all the mediaList where fileSize is less than or equal to
        defaultMediaFiltering("fileSize.lessThanOrEqual=" + DEFAULT_FILE_SIZE, "fileSize.lessThanOrEqual=" + SMALLER_FILE_SIZE);
    }

    @Test
    @Transactional
    void getAllMediasByFileSizeIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedMedia = mediaRepository.saveAndFlush(media);

        // Get all the mediaList where fileSize is less than
        defaultMediaFiltering("fileSize.lessThan=" + UPDATED_FILE_SIZE, "fileSize.lessThan=" + DEFAULT_FILE_SIZE);
    }

    @Test
    @Transactional
    void getAllMediasByFileSizeIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedMedia = mediaRepository.saveAndFlush(media);

        // Get all the mediaList where fileSize is greater than
        defaultMediaFiltering("fileSize.greaterThan=" + SMALLER_FILE_SIZE, "fileSize.greaterThan=" + DEFAULT_FILE_SIZE);
    }

    @Test
    @Transactional
    void getAllMediasByStorageProviderIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedMedia = mediaRepository.saveAndFlush(media);

        // Get all the mediaList where storageProvider equals to
        defaultMediaFiltering("storageProvider.equals=" + DEFAULT_STORAGE_PROVIDER, "storageProvider.equals=" + UPDATED_STORAGE_PROVIDER);
    }

    @Test
    @Transactional
    void getAllMediasByStorageProviderIsInShouldWork() throws Exception {
        // Initialize the database
        insertedMedia = mediaRepository.saveAndFlush(media);

        // Get all the mediaList where storageProvider in
        defaultMediaFiltering(
            "storageProvider.in=" + DEFAULT_STORAGE_PROVIDER + "," + UPDATED_STORAGE_PROVIDER,
            "storageProvider.in=" + UPDATED_STORAGE_PROVIDER
        );
    }

    @Test
    @Transactional
    void getAllMediasByStorageProviderIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedMedia = mediaRepository.saveAndFlush(media);

        // Get all the mediaList where storageProvider is not null
        defaultMediaFiltering("storageProvider.specified=true", "storageProvider.specified=false");
    }

    @Test
    @Transactional
    void getAllMediasByStorageKeyIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedMedia = mediaRepository.saveAndFlush(media);

        // Get all the mediaList where storageKey equals to
        defaultMediaFiltering("storageKey.equals=" + DEFAULT_STORAGE_KEY, "storageKey.equals=" + UPDATED_STORAGE_KEY);
    }

    @Test
    @Transactional
    void getAllMediasByStorageKeyIsInShouldWork() throws Exception {
        // Initialize the database
        insertedMedia = mediaRepository.saveAndFlush(media);

        // Get all the mediaList where storageKey in
        defaultMediaFiltering("storageKey.in=" + DEFAULT_STORAGE_KEY + "," + UPDATED_STORAGE_KEY, "storageKey.in=" + UPDATED_STORAGE_KEY);
    }

    @Test
    @Transactional
    void getAllMediasByStorageKeyIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedMedia = mediaRepository.saveAndFlush(media);

        // Get all the mediaList where storageKey is not null
        defaultMediaFiltering("storageKey.specified=true", "storageKey.specified=false");
    }

    @Test
    @Transactional
    void getAllMediasByStorageKeyContainsSomething() throws Exception {
        // Initialize the database
        insertedMedia = mediaRepository.saveAndFlush(media);

        // Get all the mediaList where storageKey contains
        defaultMediaFiltering("storageKey.contains=" + DEFAULT_STORAGE_KEY, "storageKey.contains=" + UPDATED_STORAGE_KEY);
    }

    @Test
    @Transactional
    void getAllMediasByStorageKeyNotContainsSomething() throws Exception {
        // Initialize the database
        insertedMedia = mediaRepository.saveAndFlush(media);

        // Get all the mediaList where storageKey does not contain
        defaultMediaFiltering("storageKey.doesNotContain=" + UPDATED_STORAGE_KEY, "storageKey.doesNotContain=" + DEFAULT_STORAGE_KEY);
    }

    @Test
    @Transactional
    void getAllMediasByUrlIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedMedia = mediaRepository.saveAndFlush(media);

        // Get all the mediaList where url equals to
        defaultMediaFiltering("url.equals=" + DEFAULT_URL, "url.equals=" + UPDATED_URL);
    }

    @Test
    @Transactional
    void getAllMediasByUrlIsInShouldWork() throws Exception {
        // Initialize the database
        insertedMedia = mediaRepository.saveAndFlush(media);

        // Get all the mediaList where url in
        defaultMediaFiltering("url.in=" + DEFAULT_URL + "," + UPDATED_URL, "url.in=" + UPDATED_URL);
    }

    @Test
    @Transactional
    void getAllMediasByUrlIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedMedia = mediaRepository.saveAndFlush(media);

        // Get all the mediaList where url is not null
        defaultMediaFiltering("url.specified=true", "url.specified=false");
    }

    @Test
    @Transactional
    void getAllMediasByUrlContainsSomething() throws Exception {
        // Initialize the database
        insertedMedia = mediaRepository.saveAndFlush(media);

        // Get all the mediaList where url contains
        defaultMediaFiltering("url.contains=" + DEFAULT_URL, "url.contains=" + UPDATED_URL);
    }

    @Test
    @Transactional
    void getAllMediasByUrlNotContainsSomething() throws Exception {
        // Initialize the database
        insertedMedia = mediaRepository.saveAndFlush(media);

        // Get all the mediaList where url does not contain
        defaultMediaFiltering("url.doesNotContain=" + UPDATED_URL, "url.doesNotContain=" + DEFAULT_URL);
    }

    @Test
    @Transactional
    void getAllMediasByStatusIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedMedia = mediaRepository.saveAndFlush(media);

        // Get all the mediaList where status equals to
        defaultMediaFiltering("status.equals=" + DEFAULT_STATUS, "status.equals=" + UPDATED_STATUS);
    }

    @Test
    @Transactional
    void getAllMediasByStatusIsInShouldWork() throws Exception {
        // Initialize the database
        insertedMedia = mediaRepository.saveAndFlush(media);

        // Get all the mediaList where status in
        defaultMediaFiltering("status.in=" + DEFAULT_STATUS + "," + UPDATED_STATUS, "status.in=" + UPDATED_STATUS);
    }

    @Test
    @Transactional
    void getAllMediasByStatusIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedMedia = mediaRepository.saveAndFlush(media);

        // Get all the mediaList where status is not null
        defaultMediaFiltering("status.specified=true", "status.specified=false");
    }

    @Test
    @Transactional
    void getAllMediasByCreatedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedMedia = mediaRepository.saveAndFlush(media);

        // Get all the mediaList where createdAt equals to
        defaultMediaFiltering("createdAt.equals=" + DEFAULT_CREATED_AT, "createdAt.equals=" + UPDATED_CREATED_AT);
    }

    @Test
    @Transactional
    void getAllMediasByCreatedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedMedia = mediaRepository.saveAndFlush(media);

        // Get all the mediaList where createdAt in
        defaultMediaFiltering("createdAt.in=" + DEFAULT_CREATED_AT + "," + UPDATED_CREATED_AT, "createdAt.in=" + UPDATED_CREATED_AT);
    }

    @Test
    @Transactional
    void getAllMediasByCreatedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedMedia = mediaRepository.saveAndFlush(media);

        // Get all the mediaList where createdAt is not null
        defaultMediaFiltering("createdAt.specified=true", "createdAt.specified=false");
    }

    @Test
    @Transactional
    void getAllMediasByUpdatedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedMedia = mediaRepository.saveAndFlush(media);

        // Get all the mediaList where updatedAt equals to
        defaultMediaFiltering("updatedAt.equals=" + DEFAULT_UPDATED_AT, "updatedAt.equals=" + UPDATED_UPDATED_AT);
    }

    @Test
    @Transactional
    void getAllMediasByUpdatedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedMedia = mediaRepository.saveAndFlush(media);

        // Get all the mediaList where updatedAt in
        defaultMediaFiltering("updatedAt.in=" + DEFAULT_UPDATED_AT + "," + UPDATED_UPDATED_AT, "updatedAt.in=" + UPDATED_UPDATED_AT);
    }

    @Test
    @Transactional
    void getAllMediasByUpdatedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedMedia = mediaRepository.saveAndFlush(media);

        // Get all the mediaList where updatedAt is not null
        defaultMediaFiltering("updatedAt.specified=true", "updatedAt.specified=false");
    }

    private void defaultMediaFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultMediaShouldBeFound(shouldBeFound);
        defaultMediaShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultMediaShouldBeFound(String filter) throws Exception {
        restMediaMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(media.getId().toString())))
            .andExpect(jsonPath("$.[*].ownerId").value(hasItem(DEFAULT_OWNER_ID.toString())))
            .andExpect(jsonPath("$.[*].fileName").value(hasItem(DEFAULT_FILE_NAME)))
            .andExpect(jsonPath("$.[*].mediaType").value(hasItem(DEFAULT_MEDIA_TYPE.toString())))
            .andExpect(jsonPath("$.[*].mimeType").value(hasItem(DEFAULT_MIME_TYPE)))
            .andExpect(jsonPath("$.[*].fileSize").value(hasItem(DEFAULT_FILE_SIZE.intValue())))
            .andExpect(jsonPath("$.[*].storageProvider").value(hasItem(DEFAULT_STORAGE_PROVIDER.toString())))
            .andExpect(jsonPath("$.[*].storageKey").value(hasItem(DEFAULT_STORAGE_KEY)))
            .andExpect(jsonPath("$.[*].url").value(hasItem(DEFAULT_URL)))
            .andExpect(jsonPath("$.[*].status").value(hasItem(DEFAULT_STATUS.toString())))
            .andExpect(jsonPath("$.[*].createdAt").value(hasItem(DEFAULT_CREATED_AT.toString())))
            .andExpect(jsonPath("$.[*].updatedAt").value(hasItem(DEFAULT_UPDATED_AT.toString())));

        // Check, that the count call also returns 1
        restMediaMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultMediaShouldNotBeFound(String filter) throws Exception {
        restMediaMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restMediaMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingMedia() throws Exception {
        // Get the media
        restMediaMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingMedia() throws Exception {
        // Initialize the database
        insertedMedia = mediaRepository.saveAndFlush(media);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the media
        Media updatedMedia = mediaRepository.findById(media.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedMedia are not directly saved in db
        em.detach(updatedMedia);
        updatedMedia
            .ownerId(UPDATED_OWNER_ID)
            .fileName(UPDATED_FILE_NAME)
            .mediaType(UPDATED_MEDIA_TYPE)
            .mimeType(UPDATED_MIME_TYPE)
            .fileSize(UPDATED_FILE_SIZE)
            .storageProvider(UPDATED_STORAGE_PROVIDER)
            .storageKey(UPDATED_STORAGE_KEY)
            .url(UPDATED_URL)
            .status(UPDATED_STATUS)
            .createdAt(UPDATED_CREATED_AT)
            .updatedAt(UPDATED_UPDATED_AT);
        MediaDTO mediaDTO = mediaMapper.toDto(updatedMedia);

        restMediaMockMvc
            .perform(
                put(ENTITY_API_URL_ID, mediaDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(mediaDTO))
            )
            .andExpect(status().isOk());

        // Validate the Media in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedMediaToMatchAllProperties(updatedMedia);
    }

    @Test
    @Transactional
    void putNonExistingMedia() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        media.setId(UUID.randomUUID());

        // Create the Media
        MediaDTO mediaDTO = mediaMapper.toDto(media);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restMediaMockMvc
            .perform(
                put(ENTITY_API_URL_ID, mediaDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(mediaDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Media in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchMedia() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        media.setId(UUID.randomUUID());

        // Create the Media
        MediaDTO mediaDTO = mediaMapper.toDto(media);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restMediaMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(mediaDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Media in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamMedia() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        media.setId(UUID.randomUUID());

        // Create the Media
        MediaDTO mediaDTO = mediaMapper.toDto(media);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restMediaMockMvc
            .perform(put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(mediaDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Media in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateMediaWithPatch() throws Exception {
        // Initialize the database
        insertedMedia = mediaRepository.saveAndFlush(media);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the media using partial update
        Media partialUpdatedMedia = new Media();
        partialUpdatedMedia.setId(media.getId());

        partialUpdatedMedia.fileSize(UPDATED_FILE_SIZE).url(UPDATED_URL).updatedAt(UPDATED_UPDATED_AT);

        restMediaMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedMedia.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedMedia))
            )
            .andExpect(status().isOk());

        // Validate the Media in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertMediaUpdatableFieldsEquals(createUpdateProxyForBean(partialUpdatedMedia, media), getPersistedMedia(media));
    }

    @Test
    @Transactional
    void fullUpdateMediaWithPatch() throws Exception {
        // Initialize the database
        insertedMedia = mediaRepository.saveAndFlush(media);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the media using partial update
        Media partialUpdatedMedia = new Media();
        partialUpdatedMedia.setId(media.getId());

        partialUpdatedMedia
            .ownerId(UPDATED_OWNER_ID)
            .fileName(UPDATED_FILE_NAME)
            .mediaType(UPDATED_MEDIA_TYPE)
            .mimeType(UPDATED_MIME_TYPE)
            .fileSize(UPDATED_FILE_SIZE)
            .storageProvider(UPDATED_STORAGE_PROVIDER)
            .storageKey(UPDATED_STORAGE_KEY)
            .url(UPDATED_URL)
            .status(UPDATED_STATUS)
            .createdAt(UPDATED_CREATED_AT)
            .updatedAt(UPDATED_UPDATED_AT);

        restMediaMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedMedia.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedMedia))
            )
            .andExpect(status().isOk());

        // Validate the Media in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertMediaUpdatableFieldsEquals(partialUpdatedMedia, getPersistedMedia(partialUpdatedMedia));
    }

    @Test
    @Transactional
    void patchNonExistingMedia() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        media.setId(UUID.randomUUID());

        // Create the Media
        MediaDTO mediaDTO = mediaMapper.toDto(media);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restMediaMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, mediaDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(mediaDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Media in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchMedia() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        media.setId(UUID.randomUUID());

        // Create the Media
        MediaDTO mediaDTO = mediaMapper.toDto(media);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restMediaMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(mediaDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Media in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamMedia() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        media.setId(UUID.randomUUID());

        // Create the Media
        MediaDTO mediaDTO = mediaMapper.toDto(media);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restMediaMockMvc
            .perform(patch(ENTITY_API_URL).with(csrf()).contentType("application/merge-patch+json").content(om.writeValueAsBytes(mediaDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Media in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteMedia() throws Exception {
        // Initialize the database
        insertedMedia = mediaRepository.saveAndFlush(media);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the media
        restMediaMockMvc
            .perform(delete(ENTITY_API_URL_ID, media.getId().toString()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return mediaRepository.count();
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

    protected Media getPersistedMedia(Media media) {
        return mediaRepository.findById(media.getId()).orElseThrow();
    }

    protected void assertPersistedMediaToMatchAllProperties(Media expectedMedia) {
        assertMediaAllPropertiesEquals(expectedMedia, getPersistedMedia(expectedMedia));
    }

    protected void assertPersistedMediaToMatchUpdatableProperties(Media expectedMedia) {
        assertMediaAllUpdatablePropertiesEquals(expectedMedia, getPersistedMedia(expectedMedia));
    }
}
