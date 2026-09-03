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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.PaginationUtil;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link com.minh.fakebook.media.domain.Media}.
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
     *
     * @param mediaDTO the mediaDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new mediaDTO, or with status {@code 400 (Bad Request)} if the media has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
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
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Media in body.
     */
    @GetMapping("")
    public ResponseEntity<List<MediaDTO>> getAllMedias(
        MediaCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get Medias by criteria: {}", criteria);

        Page<MediaDTO> page = mediaQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /media/count} : count all the medias.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countMedias(MediaCriteria criteria) {
        LOG.debug("REST request to count Medias by criteria: {}", criteria);
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
}
