package com.minh.fakebook.post.web.rest;

import com.minh.fakebook.post.repository.PostReactionRepository;
import com.minh.fakebook.post.service.PostReactionQueryService;
import com.minh.fakebook.post.service.PostReactionService;
import com.minh.fakebook.post.service.criteria.PostReactionCriteria;
import com.minh.fakebook.post.service.dto.PostReactionDTO;
import com.minh.fakebook.post.web.rest.errors.BadRequestAlertException;
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
 * REST controller for managing {@link com.minh.fakebook.post.domain.PostReaction}.
 */
@RestController
@RequestMapping("/api/post-reactions")
public class PostReactionResource {

    private static final Logger LOG = LoggerFactory.getLogger(PostReactionResource.class);

    private static final String ENTITY_NAME = "postServicePostReaction";

    @Value("${jhipster.clientApp.name:postService}")
    private String applicationName;

    private final PostReactionService postReactionService;

    private final PostReactionRepository postReactionRepository;

    private final PostReactionQueryService postReactionQueryService;

    public PostReactionResource(
        PostReactionService postReactionService,
        PostReactionRepository postReactionRepository,
        PostReactionQueryService postReactionQueryService
    ) {
        this.postReactionService = postReactionService;
        this.postReactionRepository = postReactionRepository;
        this.postReactionQueryService = postReactionQueryService;
    }

    /**
     * {@code POST  /post-reactions} : Create a new postReaction.
     *
     * @param postReactionDTO the postReactionDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new postReactionDTO, or with status {@code 400 (Bad Request)} if the postReaction has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<PostReactionDTO> createPostReaction(@Valid @RequestBody PostReactionDTO postReactionDTO)
        throws URISyntaxException {
        LOG.debug("REST request to save PostReaction : {}", postReactionDTO);
        if (postReactionDTO.getId() != null) {
            throw new BadRequestAlertException("A new postReaction cannot already have an ID", ENTITY_NAME, "idexists");
        }
        postReactionDTO = postReactionService.save(postReactionDTO);
        return ResponseEntity.created(new URI("/api/post-reactions/" + postReactionDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, postReactionDTO.getId().toString()))
            .body(postReactionDTO);
    }

    /**
     * {@code PUT  /post-reactions/:id} : Updates an existing postReaction.
     *
     * @param id the id of the postReactionDTO to save.
     * @param postReactionDTO the postReactionDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated postReactionDTO,
     * or with status {@code 400 (Bad Request)} if the postReactionDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the postReactionDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<PostReactionDTO> updatePostReaction(
        @PathVariable(value = "id", required = false) final UUID id,
        @Valid @RequestBody PostReactionDTO postReactionDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update PostReaction : {}, {}", id, postReactionDTO);
        if (postReactionDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, postReactionDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!postReactionRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        postReactionDTO = postReactionService.update(postReactionDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, postReactionDTO.getId().toString()))
            .body(postReactionDTO);
    }

    /**
     * {@code PATCH  /post-reactions/:id} : Partial updates given fields of an existing postReaction, field will ignore if it is null
     *
     * @param id the id of the postReactionDTO to save.
     * @param postReactionDTO the postReactionDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated postReactionDTO,
     * or with status {@code 400 (Bad Request)} if the postReactionDTO is not valid,
     * or with status {@code 404 (Not Found)} if the postReactionDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the postReactionDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<PostReactionDTO> partialUpdatePostReaction(
        @PathVariable(value = "id", required = false) final UUID id,
        @NotNull @RequestBody PostReactionDTO postReactionDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update PostReaction partially : {}, {}", id, postReactionDTO);
        if (postReactionDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, postReactionDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!postReactionRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<PostReactionDTO> result = postReactionService.partialUpdate(postReactionDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, postReactionDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /post-reactions} : get all the Post Reactions.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Post Reactions in body.
     */
    @GetMapping("")
    public ResponseEntity<List<PostReactionDTO>> getAllPostReactions(
        PostReactionCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get PostReactions by criteria: {}", criteria);

        Page<PostReactionDTO> page = postReactionQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /post-reactions/count} : count all the postReactions.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countPostReactions(PostReactionCriteria criteria) {
        LOG.debug("REST request to count PostReactions by criteria: {}", criteria);
        return ResponseEntity.ok().body(postReactionQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /post-reactions/:id} : get the "id" postReaction.
     *
     * @param id the id of the postReactionDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the postReactionDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<PostReactionDTO> getPostReaction(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get PostReaction : {}", id);
        Optional<PostReactionDTO> postReactionDTO = postReactionService.findOne(id);
        return ResponseUtil.wrapOrNotFound(postReactionDTO);
    }

    /**
     * {@code DELETE  /post-reactions/:id} : delete the "id" postReaction.
     *
     * @param id the id of the postReactionDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePostReaction(@PathVariable("id") UUID id) {
        LOG.debug("REST request to delete PostReaction : {}", id);
        postReactionService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
