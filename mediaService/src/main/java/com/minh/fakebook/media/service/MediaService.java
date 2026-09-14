package com.minh.fakebook.media.service;

import com.minh.fakebook.media.domain.Media;
import com.minh.fakebook.media.domain.enumeration.MediaStatus;
import com.minh.fakebook.media.domain.enumeration.MediaType;
import com.minh.fakebook.media.domain.enumeration.StorageProvider;
import com.minh.fakebook.media.repository.MediaRepository;
import com.minh.fakebook.media.security.AuthoritiesConstants;
import com.minh.fakebook.media.service.dto.FileUploadResult;
import com.minh.fakebook.media.service.dto.MediaDTO;
import com.minh.fakebook.media.service.mapper.MediaMapper;
import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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

            Optional<Media> mediaOpt = mediaRepository.findById(id);
            if (mediaOpt.isEmpty()) {
                return Optional.empty();
            }

            Media media = mediaOpt.get();

            // 1. Identify if the current request is from a Guest or an Authenticated User
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            boolean isGuest = auth == null || auth instanceof AnonymousAuthenticationToken;

            String currentUserId = null;
            if (!isGuest) {
                currentUserId = ((JwtAuthenticationToken) auth).getToken().getSubject();
            }

            boolean isOwner = !isGuest && media.getOwnerId().toString().equals(currentUserId);

            if (media.getStatus() != MediaStatus.ACTIVE)
  {
                // If media is not ACTIVE (e.g., DELETED), ONLY the owner can view it.
                if (!isOwner) {
                    LOG.debug("Access Denied: User/Guest is not the owner of this media.");
                    throw new AccessDeniedException("Error: You do not have permission to view this media.");
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
            Optional<Media> mediaOpt = mediaRepository.findById(id);
            if (mediaOpt.isEmpty()) {
                return; // Media not found, safely return
            }
            Media media = mediaOpt.get();

            // 2. Authentication check
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || auth instanceof AnonymousAuthenticationToken) {
                throw new AccessDeniedException("Error: You must be logged in to delete media.");
            }

            // 3. Extract user ID and check roles 
            String currentUserId = ((JwtAuthenticationToken) auth).getToken().getSubject();
            boolean isOwner = media.getOwnerId().toString().equals(currentUserId);

            boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals(AuthoritiesConstants.ADMIN));

            // 4. Permission: Must be Owner or Admin
            if (!isOwner && !isAdmin) {
                LOG.warn("User {} attempted to delete media {} without permission", currentUserId,id);
                throw new AccessDeniedException("Error: Only the owner or an admin can delete this media.");
            }

            // 5. Perform Soft Delete (Change status to DELETED)
            media.setStatus(MediaStatus.DELETED);
            media.setUpdatedAt(Instant.now());
            mediaRepository.save(media);

            LOG.debug("Media {} successfully deleted by user {}", id, currentUserId);
        }

    /**
     * Uploads a media file to Cloudinary and saves metadata to DB.
     *
     * @param file the multipart file to upload
     * @return the persisted MediaDTO
     */
    public MediaDTO uploadMedia(
            MultipartFile file) {
        try {
            // 1. Extract user authentication and get current user ID
            Authentication auth = SecurityContextHolder
                    .getContext().getAuthentication();
            String sub = ((JwtAuthenticationToken) auth)
                    .getToken().getSubject();
            UUID currentUserId = UUID.fromString(sub);

            // 2. Validate file 
            if (file.isEmpty()) {
                throw new IllegalArgumentException("Error: File is empty.");
            }
            String contentType = file.getContentType();
            if (contentType == null || (!contentType.startsWith("image/") && !contentType.startsWith("video/"))) {
                throw new IllegalArgumentException("Error: Only images and videos are supported.");
            }

            // Determine media type based on content type
            MediaType mediaType = contentType.startsWith("video/")
                    ? MediaType.VIDEO
                    : MediaType.IMAGE;

            // 3. Upload file to Cloudinary
            String folder = "fakebook/users/" + currentUserId.toString();
            FileUploadResult uploadResult = fileStorageService.uploadFile(file,
                    folder);

            // 4. Save Metadata to DB
            Media media = new Media();
            media.setOwnerId(currentUserId);
            media.setFileName(file.getOriginalFilename());
            media.setMediaType(mediaType);
            media.setMimeType(contentType);
            media.setFileSize(file.getSize());
            media.setStorageProvider(StorageProvider.CLOUDINARY);
            media.setStorageKey(uploadResult.storageKey());
            media.setUrl(uploadResult.url());
            media.setStatus(MediaStatus.ACTIVE);
            media.setCreatedAt(Instant.now());

            media = mediaRepository.save(media);
            return mediaMapper.toDto(media);

        } catch (IOException e) {
            LOG.error("Failed to upload file to Cloudinary", e);
            throw new RuntimeException("Error: Could not upload file.", e);
        }
    }
}
