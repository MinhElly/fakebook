package com.minh.fakebook.media.service;

import com.minh.fakebook.media.domain.Media;
import com.minh.fakebook.media.domain.enumeration.MediaPurpose;
import com.minh.fakebook.media.domain.enumeration.MediaStatus;
import com.minh.fakebook.media.domain.enumeration.MediaType;
import com.minh.fakebook.media.domain.enumeration.StorageProvider;
import com.minh.fakebook.media.repository.MediaRepository;
import com.minh.fakebook.media.security.AuthoritiesConstants;
import com.minh.fakebook.media.service.dto.FileUploadResult;
import com.minh.fakebook.media.service.dto.MediaDTO;
import com.minh.fakebook.media.service.mapper.MediaMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
 * Service Implementation for managing
 * {@link com.minh.fakebook.media.domain.Media}.
 */
@Service
@Transactional
public class MediaService {

    private static final Logger LOG = LoggerFactory.getLogger(MediaService.class);

    private final MediaRepository mediaRepository;

    private final MediaMapper mediaMapper;

    private final FileStorageService fileStorageService;
    private final PostMediaAccess postAccess;

    public MediaService(MediaRepository mediaRepository, MediaMapper mediaMapper,
            FileStorageService fileStorageService, PostMediaAccess postAccess) {
        this.mediaRepository = mediaRepository;
        this.mediaMapper = mediaMapper;
        this.fileStorageService = fileStorageService;
        this.postAccess = postAccess;
    }

    /**
     * Save a media metadata directly.
     * Restricted to administrators to prevent arbitrary metadata creation.
     *
     * @param mediaDTO the entity to save.
     * @return the persisted entity.
     */
    public MediaDTO save(MediaDTO mediaDTO) {
        LOG.debug("Request to save Media : {}", mediaDTO);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(AuthoritiesConstants.ADMIN));
        if (!isAdmin) {
            throw new AccessDeniedException("Only administrators can directly register media metadata.");
        }
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
        Media existingMedia = mediaRepository.findById(mediaDTO.getId())
                .orElseThrow(() -> new IllegalArgumentException("Media not found"));
        checkOwnershipOrAdmin(existingMedia);
        existingMedia.setStatus(mediaDTO.getStatus());
        existingMedia.setPurpose(mediaDTO.getPurpose());
        return mediaMapper.toDto(mediaRepository.save(existingMedia));
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
                    checkOwnershipOrAdmin(existingMedia);
                    if (mediaDTO.getStatus() != null) {
                        existingMedia.setStatus(mediaDTO.getStatus());
                    }
                    if (mediaDTO.getPurpose() != null) {
                        existingMedia.setPurpose(mediaDTO.getPurpose());
                    }
                    return existingMedia;
                })
                .map(mediaRepository::save)
                .map(mediaMapper::toDto);
    }

    /**
     * Get one media by id with strict Role-Based Access Control.
     *
     * @param id the id of the entity.
     * @return the entity.
     * @throws AccessDeniedException if unauthorized.
     */
    @Transactional(readOnly = true)
    public Optional<MediaDTO> findOne(UUID id) {
        LOG.debug("Request to get Media : {}", id);

        Optional<Media> mediaOpt = mediaRepository.findById(id);
        if (mediaOpt.isEmpty()) {
            return Optional.empty();
        }

        Media media = mediaOpt.orElseThrow();

        // 1. Identify if the current request is from a Guest or an Authenticated User
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isGuest = auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken
                || "anonymousUser".equals(auth.getPrincipal());
        String currentUserId = null;
        boolean isAdmin = false;
        if (!isGuest) {
            isAdmin = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals(AuthoritiesConstants.ADMIN));
            if (auth instanceof JwtAuthenticationToken jwtToken) {
                currentUserId = jwtToken.getToken().getSubject();
            } else if (auth.getPrincipal() instanceof String s) {
                currentUserId = s;
            }
        }
        boolean isOwner = !isGuest && currentUserId != null && media.getOwnerId().toString().equals(currentUserId);

        if (!isAdmin && !isOwner) {
            boolean profileAsset = media.getPurpose() == MediaPurpose.AVATAR || media.getPurpose() == MediaPurpose.COVER;
            boolean parentGrant = media.getPurpose() == MediaPurpose.POST && media.getStatus() == MediaStatus.ACTIVE && postAccess.canRead(id);
            if (media.getStatus() != MediaStatus.ACTIVE || (!profileAsset && !parentGrant)) {
                throw new AccessDeniedException("Media is not readable by this viewer");
            }
        }
        return Optional.of(mediaMapper.toDto(media));
    }

    /**
     * Delete the media by id (Soft Delete).
     * Only the Owner or an Admin can delete.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete Media : {}", id);

        // 1. Retrieve the Media
        Media media = mediaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Media not found"));
        checkOwnershipOrAdmin(media);

        // 2. Authentication check
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth instanceof AnonymousAuthenticationToken) {
            throw new AccessDeniedException("Error: You must be logged in to delete media.");
        }

        // 3. Extract user ID and check roles
        String currentUserId = null;
        if (auth instanceof JwtAuthenticationToken jwtToken) {
            currentUserId = jwtToken.getToken().getSubject();
        } else {
            currentUserId = auth.getName();
        }
        boolean isOwner = media.getOwnerId().toString().equals(currentUserId);

        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals(AuthoritiesConstants.ADMIN));

        // 4. Permission: Must be Owner or Admin
        if (!isOwner && !isAdmin) {
            LOG.warn("User {} attempted to delete media {} without permission", currentUserId, id);
            throw new AccessDeniedException("Error: Only the owner or an admin can delete this media.");
        }

        // 5. Perform Soft Delete (Change status to DELETED)
        media.setStatus(MediaStatus.DELETED);
        media.setUpdatedAt(Instant.now());
        mediaRepository.save(media);

        LOG.debug("Media {} successfully deleted by user {}", id, currentUserId);
    }

    /**
     * Delete media by system process (e.g. Kafka event cleanup).
     * Bypasses HTTP SecurityContext check and deletes physical file from storage
     * provider.
     *
     * @param id the id of the media entity.
     */
    public void deleteBySystem(UUID id) {
        LOG.debug("System request to cleanup Media : {}", id);
        Optional<Media> mediaOpt = mediaRepository.findById(id);
        if (mediaOpt.isEmpty()) {
            LOG.debug("Media ID {} not found for system cleanup.", id);
            return;
        }
        Media media = mediaOpt.orElseThrow();

        // 1. Physical file cleanup from Cloudinary / Storage Provider
        try {
            if (media.getStorageKey() != null) {
                fileStorageService.deleteFile(media.getStorageKey());
                LOG.info("Successfully deleted physical media file from storage: {}", media.getStorageKey());
            }
        } catch (Exception e) {
            LOG.error("Failed to delete physical media file with key: {}", media.getStorageKey(), e);
            throw new IllegalStateException("Storage cleanup failed for media " + id, e);
        }

        // 2. Mark media status as DELETED
        media.setStatus(MediaStatus.DELETED);
        media.setUpdatedAt(Instant.now());
        mediaRepository.save(media);
        LOG.info("System successfully marked Media {} as DELETED.", id);
    }

    /**
     * Uploads a media file to Cloudinary and saves metadata to DB.
     *
     * @param file the multipart file to upload
     * @return the persisted MediaDTO
     */
    public MediaDTO uploadMedia(MultipartFile file, MediaPurpose purpose) {
        try {
            // 1. Extract user authentication and get current user ID
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || auth instanceof AnonymousAuthenticationToken) {
                throw new AccessDeniedException("Error: You must be logged in to upload media.");
            }

            String sub = null;
            if (auth instanceof JwtAuthenticationToken jwtToken) {
                sub = jwtToken.getToken().getSubject();
            } else {
                sub = auth.getName();
            }

            UUID currentUserId;
            try {
                currentUserId = UUID.fromString(sub);
            } catch (Exception e) {
                currentUserId = UUID.nameUUIDFromBytes(sub.getBytes(StandardCharsets.UTF_8));
            }

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

            // 3. Upload file to storage provider
            String folder = "fakebook/users/" + currentUserId.toString();
            FileUploadResult uploadResult = fileStorageService.uploadFile(file, folder);

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
            media.setPurpose(purpose);

            media = mediaRepository.save(media);
            return mediaMapper.toDto(media);

        } catch (AccessDeniedException | IllegalArgumentException e) {
            throw e;
        } catch (StorageServiceUnavailableException e) {
            throw e;
        } catch (Exception e) {
            LOG.error("Failed to upload file", e);
            throw new RuntimeException("Error: Could not upload file.", e);
        }
    }

    private void checkOwnershipOrAdmin(Media media) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("User is not authenticated");
        }
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(AuthoritiesConstants.ADMIN));
        if (isAdmin)
            return;

        if (auth instanceof JwtAuthenticationToken jwt) {
            String currentUserId = jwt.getToken().getSubject();
            if (!media.getOwnerId().toString().equals(currentUserId)) {
                throw new AccessDeniedException("You do not own this media resource");
            }
        } else {
            throw new AccessDeniedException("Invalid authentication token");
        }
    }

}
