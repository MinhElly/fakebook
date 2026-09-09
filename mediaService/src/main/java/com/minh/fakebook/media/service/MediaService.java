package com.minh.fakebook.media.service;

import com.minh.fakebook.media.domain.Media;
import com.minh.fakebook.media.repository.MediaRepository;
import com.minh.fakebook.media.service.dto.MediaDTO;
import com.minh.fakebook.media.service.mapper.MediaMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.minh.fakebook.media.domain.Media}.
 */
@Service
@Transactional
public class MediaService {

    private static final Logger LOG = LoggerFactory.getLogger(MediaService.class);

    private final MediaRepository mediaRepository;

    private final MediaMapper mediaMapper;

    private final FileStorageService fileStorageService;

    public MediaService(MediaRepository mediaRepository, MediaMapper mediaMapper, FileStorageService fileStorageService) {
        this.mediaRepository = mediaRepository;
        this.mediaMapper = mediaMapper;
        this.fileStorageService = fileStorageService;
    }

    /**
     * Save a media.
     *
     * @param mediaDTO the entity to save.
     * @return the persisted entity.
     */
    public MediaDTO save(MediaDTO mediaDTO) {
        LOG.debug("Request to save Media : {}", mediaDTO);
        Media media = mediaMapper.toEntity(mediaDTO);
        media = mediaRepository.save(media);
        return mediaMapper.toDto(media);
    }

    /**
     * Update a media.
     *
     * @param mediaDTO the entity to save.
     * @return the persisted entity.
     */
    public MediaDTO update(MediaDTO mediaDTO) {
        LOG.debug("Request to update Media : {}", mediaDTO);
        Media media = mediaMapper.toEntity(mediaDTO);
        media = mediaRepository.save(media);
        return mediaMapper.toDto(media);
    }

    /**
     * Partially update a media.
     *
     * @param mediaDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<MediaDTO> partialUpdate(MediaDTO mediaDTO) {
        LOG.debug("Request to partially update Media : {}", mediaDTO);

        return mediaRepository
            .findById(mediaDTO.getId())
            .map(existingMedia -> {
                mediaMapper.partialUpdate(existingMedia, mediaDTO);

                return existingMedia;
            })
            .map(mediaRepository::save)
            .map(mediaMapper::toDto);
    }

    /**
         * Get one media by id with strict Role-Based Access Control (Rule 21).
         *
         * @param id the id of the entity.
         * @return the entity.
         * @throws org.springframework.security.access.AccessDeniedException if unauthorized.
         */
        @Transactional(readOnly = true)
        public Optional<MediaDTO> findOne(UUID id) {
            LOG.debug("Request to get Media : {}", id);

            Optional<com.minh.fakebook.media.domain.Media> mediaOpt = mediaRepository.findById(id);
            if (mediaOpt.isEmpty()) {
                return Optional.empty();
            }

            com.minh.fakebook.media.domain.Media media = mediaOpt.get();

            // 1. Identify if the current request is from a Guest or an Authenticated User
            org.springframework.security.core.Authentication auth = org.springframework.security.
  core.context.SecurityContextHolder.getContext().getAuthentication();
            boolean isGuest = auth == null || auth instanceof org.springframework.security.
  authentication.AnonymousAuthenticationToken;

            String currentUserId = null;
            if (!isGuest) {
                currentUserId = ((org.springframework.security.oauth2.server.resource.
  authentication.JwtAuthenticationToken) auth).getToken().getSubject();
            }

            boolean isOwner = !isGuest && media.getOwnerId().toString().equals(currentUserId);

            if (media.getStatus() != com.minh.fakebook.media.domain.enumeration.MediaStatus.ACTIVE)
  {
                // If media is not ACTIVE (e.g., DELETED), ONLY the owner can view it.
                if (!isOwner) {
                    LOG.debug("Access Denied: User/Guest is not the owner of this media.");
                    throw new org.springframework.security.access.AccessDeniedException("Error: You do not have permission to view this media.");
                }
            }

            return Optional.of(mediaMapper.toDto(media));
        }

    /**
         * Delete the media by id (Soft Delete).
         * Enforces Rule 21: Only the Owner or an Admin can delete.
         *
         * @param id the id of the entity.
         */
        public void delete(UUID id) {
            LOG.debug("Request to delete Media : {}", id);

            // 1. Retrieve the Media
            Optional<com.minh.fakebook.media.domain.Media> mediaOpt = mediaRepository.findById(id);
            if (mediaOpt.isEmpty()) {
                return; // Media not found, safely return
            }
            com.minh.fakebook.media.domain.Media media = mediaOpt.get();

            // 2. Authentication check
            org.springframework.security.core.Authentication auth = org.springframework.security.
  core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || auth instanceof org.springframework.security.authentication.
  AnonymousAuthenticationToken) {
                throw new org.springframework.security.access.AccessDeniedException("Error: You must be logged in to delete media.");
            }

            // 3. Extract user ID and check roles 
            String currentUserId = ((org.springframework.security.oauth2.server.resource.
  authentication.JwtAuthenticationToken) auth).getToken().getSubject();
            boolean isOwner = media.getOwnerId().toString().equals(currentUserId);

            boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals(com.minh.
  fakebook.media.security.AuthoritiesConstants.ADMIN));

            // 4. Permission: Must be Owner or Admin
            if (!isOwner && !isAdmin) {
                LOG.warn("User {} attempted to delete media {} without permission", currentUserId,id);
                throw new org.springframework.security.access.AccessDeniedException("Error: Only the owner or an admin can delete this media.");
            }

            // 5. Perform Soft Delete (Change status to DELETED)
            media.setStatus(com.minh.fakebook.media.domain.enumeration.MediaStatus.DELETED);
            media.setUpdatedAt(java.time.Instant.now());
            mediaRepository.save(media);

            LOG.debug("Media {} successfully deleted by user {}", id, currentUserId);
        }

    /**
     * Uploads a media file to Cloudinary and saves metadata to DB.
     *
     * @param file the multipart file to upload
     * @return the persisted MediaDTO
     */
    public com.minh.fakebook.media.service.dto.MediaDTO uploadMedia(
            org.springframework.web.multipart.MultipartFile file) {
        try {
            // 1. Extract user authentication and get current user ID
            org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder
                    .getContext().getAuthentication();
            String sub = ((org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken) auth)
                    .getToken().getSubject();
            java.util.UUID currentUserId = java.util.UUID.fromString(sub);

            // 2. Validate file 
            if (file.isEmpty()) {
                throw new IllegalArgumentException("Error: File is empty.");
            }
            String contentType = file.getContentType();
            if (contentType == null || (!contentType.startsWith("image/") && !contentType.startsWith("video/"))) {
                throw new IllegalArgumentException("Error: Only images and videos are supported.");
            }

            // Determine media type based on content type
            com.minh.fakebook.media.domain.enumeration.MediaType mediaType = contentType.startsWith("video/")
                    ? com.minh.fakebook.media.domain.enumeration.MediaType.VIDEO
                    : com.minh.fakebook.media.domain.enumeration.MediaType.IMAGE;

            // 3. Upload file to Cloudinary
            String folder = "fakebook/users/" + currentUserId.toString();
            com.minh.fakebook.media.service.dto.FileUploadResult uploadResult = fileStorageService.uploadFile(file,
                    folder);

            // 4. Save Metadata to DB
            com.minh.fakebook.media.domain.Media media = new com.minh.fakebook.media.domain.Media();
            media.setOwnerId(currentUserId);
            media.setFileName(file.getOriginalFilename());
            media.setMediaType(mediaType);
            media.setMimeType(contentType);
            media.setFileSize(file.getSize());
            media.setStorageProvider(com.minh.fakebook.media.domain.enumeration.StorageProvider.CLOUDINARY);
            media.setStorageKey(uploadResult.storageKey());
            media.setUrl(uploadResult.url());
            media.setStatus(com.minh.fakebook.media.domain.enumeration.MediaStatus.ACTIVE);
            media.setCreatedAt(java.time.Instant.now());

            media = mediaRepository.save(media);
            return mediaMapper.toDto(media);

        } catch (java.io.IOException e) {
            LOG.error("Failed to upload file to Cloudinary", e);
            throw new RuntimeException("Error: Could not upload file.", e);
        }
    }
}
