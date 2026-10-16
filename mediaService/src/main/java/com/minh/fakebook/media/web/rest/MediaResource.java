package com.minh.fakebook.media.web.rest;

import com.minh.fakebook.media.repository.MediaRepository;
import com.minh.fakebook.media.service.MediaQueryService;
import com.minh.fakebook.media.service.MediaService;
import com.minh.fakebook.media.service.criteria.MediaCriteria;
import com.minh.fakebook.media.service.dto.MediaDTO;
import com.minh.fakebook.media.web.rest.errors.BadRequestAlertException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.PaginationUtil;
import tech.jhipster.web.util.ResponseUtil;
import com.minh.fakebook.media.domain.enumeration.MediaPurpose;
import org.springframework.http.MediaType;
import com.minh.fakebook.media.domain.Media;
import com.minh.fakebook.media.security.AuthoritiesConstants;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * REST controller for managing {@link Media}.
 */
@RestController
@RequestMapping("/api/media")
public class MediaResource {

    private static final Logger LOG = LoggerFactory.getLogger(MediaResource.class);

    private static final String ENTITY_NAME = "mediaServiceMedia";

    @Value("${jhipster.clientApp.name:mediaService}")
    private String applicationName;

    private final MediaService mediaService;

    private final MediaRepository mediaRepository;

    private final MediaQueryService mediaQueryService;

    public MediaResource(MediaService mediaService, MediaRepository mediaRepository, MediaQueryService mediaQueryService) {
        this.mediaService = mediaService;
        this.mediaRepository = mediaRepository;
        this.mediaQueryService = mediaQueryService;
    }

    /**
     * {@code POST  /media} : Create a new media.
     * Restricted to ADMIN role to prevent client metadata spoofing.
     *
     * @param mediaDTO the mediaDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new mediaDTO, or with status {@code 400 (Bad Request)} if the media has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    @PreAuthorize("hasAuthority('" + AuthoritiesConstants.ADMIN + "')")
    public ResponseEntity<MediaDTO> createMedia(@Valid @RequestBody MediaDTO mediaDTO) throws URISyntaxException {
        LOG.debug("REST request to save Media : {}", mediaDTO);
        if (mediaDTO.getId() != null) {
            throw new BadRequestAlertException("A new media cannot already have an ID", ENTITY_NAME, "idexists");
        }
        mediaDTO = mediaService.save(mediaDTO);
        return ResponseEntity.created(new URI("/api/media/" + mediaDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, mediaDTO.getId().toString()))
            .body(mediaDTO);
    }

    /**
     * {@code PUT  /media/:id} : Updates an existing media.
     *
     * @param id the id of the mediaDTO to save.
     * @param mediaDTO the mediaDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated mediaDTO,
     * or with status {@code 400 (Bad Request)} if the mediaDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the mediaDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<MediaDTO> updateMedia(
        @PathVariable(value = "id", required = false) final UUID id,
        @Valid @RequestBody MediaDTO mediaDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update Media : {}, {}", id, mediaDTO);
        if (mediaDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, mediaDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!mediaRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        mediaDTO = mediaService.update(mediaDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, mediaDTO.getId().toString()))
            .body(mediaDTO);
    }

    /**
     * {@code PATCH  /media/:id} : Partial updates given fields of an existing media, field will ignore if it is null
     *
     * @param id the id of the mediaDTO to save.
     * @param mediaDTO the mediaDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated mediaDTO,
     * or with status {@code 400 (Bad Request)} if the mediaDTO is not valid,
     * or with status {@code 404 (Not Found)} if the mediaDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the mediaDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<MediaDTO> partialUpdateMedia(
        @PathVariable(value = "id", required = false) final UUID id,
        @NotNull @RequestBody MediaDTO mediaDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update Media partially : {}, {}", id, mediaDTO);
        if (mediaDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, mediaDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!mediaRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<MediaDTO> result = mediaService.partialUpdate(mediaDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, mediaDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /media} : get all the Media.
     * Scoped to current user for non-admin callers if unscoped.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Media in body.
     */
    @GetMapping("")
    public ResponseEntity<List<MediaDTO>> getAllMedias(
        MediaCriteria criteria,
        @ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get Medias by criteria: {}", criteria);
        if (criteria.getId() != null && criteria.getId().getIn() != null) {
            if (criteria.getId().getIn().size() > 50) throw new BadRequestAlertException("At most 50 IDs", ENTITY_NAME, "toomanyids");
            var readable = new java.util.ArrayList<MediaDTO>();
            for (UUID id : criteria.getId().getIn()) {
                try { mediaService.findOne(id).ifPresent(readable::add); }
                catch (org.springframework.security.access.AccessDeniedException denied) { /* Do not enumerate denied records. */ }
            }
            return ResponseEntity.ok(readable);
        }
        scopeCriteriaForCurrentUser(criteria);
        Page<MediaDTO> page = mediaQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /media/count} : count all the medias.
     * Scoped to current user for non-admin callers if unscoped.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countMedias(MediaCriteria criteria) {
        LOG.debug("REST request to count Medias by criteria: {}", criteria);
        scopeCriteriaForCurrentUser(criteria);
        return ResponseEntity.ok().body(mediaQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /media/:id} : get the "id" media.
     *
     * @param id the id of the mediaDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the mediaDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<MediaDTO> getMedia(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get Media : {}", id);
        Optional<MediaDTO> mediaDTO = mediaService.findOne(id);
        return ResponseUtil.wrapOrNotFound(mediaDTO);
    }

    /**
     * {@code DELETE  /media/:id} : delete the "id" media.
     *
     * @param id the id of the mediaDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMedia(@PathVariable("id") UUID id) {
        LOG.debug("REST request to delete Media : {}", id);
        mediaService.delete(id);
        return ResponseEntity.noContent()
                .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
                .build();
    }

    /**
     * {@code POST  /medias/upload} : Upload a new media file (Image/Video).
     *
     * @param file the multipart file to upload.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new mediaDTO.
     */
    @PostMapping(value = "/upload", consumes = { "multipart/form-data" })
    public ResponseEntity<MediaDTO> uploadMedia(
        @RequestParam("file") MultipartFile file,
                @RequestParam(value = "purpose", defaultValue = "GENERAL") MediaPurpose purpose )
                throws URISyntaxException {
        LOG.debug("REST request to upload Media file: {}", file.getOriginalFilename());
        MediaDTO result = mediaService.uploadMedia(file, purpose);
        return ResponseEntity.created(new URI("/api/media/" + result.getId())).body(result);
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<?> getMediaFile(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get Media file : {}", id);
        Optional<MediaDTO> mediaDTO = mediaService.findOne(id);
        if (mediaDTO.isPresent() && mediaDTO.orElseThrow().getStatus() != com.minh.fakebook.media.domain.enumeration.MediaStatus.ACTIVE) return ResponseEntity.notFound().build();
        if (mediaDTO.isPresent() && mediaDTO.orElseThrow().getUrl() != null) {
            String url = mediaDTO.orElseThrow().getUrl();
            org.springframework.http.CacheControl cacheControl;
            if (mediaDTO.orElseThrow().getPurpose() == com.minh.fakebook.media.domain.enumeration.MediaPurpose.POST) {
                cacheControl = org.springframework.http.CacheControl.noStore();
            } else {
                cacheControl = org.springframework.http.CacheControl.maxAge(7, java.util.concurrent.TimeUnit.DAYS)
                        .cachePublic()
                        .immutable()
                        .staleWhileRevalidate(1, java.util.concurrent.TimeUnit.DAYS);
            }
            if (url.startsWith("data:")) {
                try {
                    String[] parts = url.split(",");
                    String header = parts[0];
                    String base64Data = parts[1];
                    String mimeType = header.substring(header.indexOf(":") + 1, header.indexOf(";"));
                    byte[] imageBytes = java.util.Base64.getDecoder().decode(base64Data);
                    return ResponseEntity.ok()
                            .cacheControl(cacheControl)
                            .contentType(org.springframework.http.MediaType.parseMediaType(mimeType))
                            .body(imageBytes);
                } catch (Exception e) {
                    LOG.error("Failed to parse base64 data URL", e);
                    return ResponseEntity.internalServerError().build();
                }
            } else {
                URI storage = URI.create(url);
                if (!"https".equals(storage.getScheme()) || !"res.cloudinary.com".equals(storage.getHost())) {
                    return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
                }
                var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
                factory.setConnectTimeout(3000); factory.setReadTimeout(30000);
                var result = org.springframework.web.client.RestClient.builder().requestFactory(factory).build()
                    .get().uri(storage).retrieve().toEntity(byte[].class);
                return ResponseEntity.ok()
                    .cacheControl(cacheControl)
                    .contentType(result.getHeaders().getContentType() == null ? MediaType.APPLICATION_OCTET_STREAM : result.getHeaders().getContentType())
                    .body(result.getBody());
            }
        }
        return ResponseEntity.notFound().build();
    }

    private void scopeCriteriaForCurrentUser(MediaCriteria criteria) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return;
        }
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(AuthoritiesConstants.ADMIN));
        if (isAdmin) {
            return;
        }
        // Non-admin users cannot do unscoped enumeration of all media
        // If criteria does not filter by ID (e.g. id.in used for feed hydration) and does not filter by ownerId,
        // scope it to the current user's ownerId
        if (auth instanceof JwtAuthenticationToken) {
            String currentUserId = null;
            if (auth instanceof JwtAuthenticationToken jwt) {
                currentUserId = jwt.getToken().getSubject();
            }
            if (currentUserId != null) {
                tech.jhipster.service.filter.UUIDFilter ownerFilter = new tech.jhipster.service.filter.UUIDFilter();
                ownerFilter.setEquals(UUID.fromString(currentUserId));
                criteria.setOwnerId(ownerFilter);
            }
        }
    }
}
