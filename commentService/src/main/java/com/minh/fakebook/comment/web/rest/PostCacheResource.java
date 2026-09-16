package com.minh.fakebook.comment.web.rest;

import com.minh.fakebook.comment.domain.PostCache;
import com.minh.fakebook.comment.repository.PostCacheRepository;
import com.minh.fakebook.comment.web.rest.errors.BadRequestAlertException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link com.minh.fakebook.comment.domain.PostCache}.
 */
@RestController
@RequestMapping("/api/post-caches")
@Transactional
public class PostCacheResource {

    private static final Logger LOG = LoggerFactory.getLogger(PostCacheResource.class);

    private static final String ENTITY_NAME = "commentServicePostCache";

    @Value("${jhipster.clientApp.name:commentService}")
    private String applicationName;

    private final PostCacheRepository postCacheRepository;

    public PostCacheResource(PostCacheRepository postCacheRepository) {
        this.postCacheRepository = postCacheRepository;
    }

    /**
     * {@code POST  /post-caches} : Create a new postCache.
     *
     * @param postCache the postCache to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new postCache, or with status {@code 400 (Bad Request)} if the postCache has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<PostCache> createPostCache(@Valid @RequestBody PostCache postCache) throws URISyntaxException {
        LOG.debug("REST request to save PostCache : {}", postCache);
        if (postCache.getId() != null) {
            throw new BadRequestAlertException("A new postCache cannot already have an ID", ENTITY_NAME, "idexists");
        }
        postCache = postCacheRepository.save(postCache);
        return ResponseEntity.created(new URI("/api/post-caches/" + postCache.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, postCache.getId().toString()))
            .body(postCache);
    }

    /**
     * {@code PUT  /post-caches/:id} : Updates an existing postCache.
     *
     * @param id the id of the postCache to save.
     * @param postCache the postCache to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated postCache,
     * or with status {@code 400 (Bad Request)} if the postCache is not valid,
     * or with status {@code 500 (Internal Server Error)} if the postCache couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<PostCache> updatePostCache(
        @PathVariable(value = "id", required = false) final UUID id,
        @Valid @RequestBody PostCache postCache
    ) throws URISyntaxException {
        LOG.debug("REST request to update PostCache : {}, {}", id, postCache);
        if (postCache.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, postCache.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!postCacheRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        postCache = postCacheRepository.save(postCache);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, postCache.getId().toString()))
            .body(postCache);
    }

    /**
     * {@code PATCH  /post-caches/:id} : Partial updates given fields of an existing postCache, field will ignore if it is null
     *
     * @param id the id of the postCache to save.
     * @param postCache the postCache to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated postCache,
     * or with status {@code 400 (Bad Request)} if the postCache is not valid,
     * or with status {@code 404 (Not Found)} if the postCache is not found,
     * or with status {@code 500 (Internal Server Error)} if the postCache couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<PostCache> partialUpdatePostCache(
        @PathVariable(value = "id", required = false) final UUID id,
        @NotNull @RequestBody PostCache postCache
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update PostCache partially : {}, {}", id, postCache);
        if (postCache.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, postCache.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!postCacheRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<PostCache> result = postCacheRepository
            .findById(postCache.getId())
            .map(existingPostCache -> {
                updateIfPresent(existingPostCache::setAuthorId, postCache.getAuthorId());
                updateIfPresent(existingPostCache::setVisibility, postCache.getVisibility());
                updateIfPresent(existingPostCache::setStatus, postCache.getStatus());

                return existingPostCache;
            })
            .map(postCacheRepository::save);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, postCache.getId().toString())
        );
    }

    /**
     * {@code GET  /post-caches} : get all the Post Caches.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Post Caches in body.
     */
    @GetMapping("")
    public List<PostCache> getAllPostCaches() {
        LOG.debug("REST request to get all PostCaches");
        return postCacheRepository.findAll();
    }

    /**
     * {@code GET  /post-caches/:id} : get the "id" postCache.
     *
     * @param id the id of the postCache to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the postCache, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<PostCache> getPostCache(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get PostCache : {}", id);
        Optional<PostCache> postCache = postCacheRepository.findById(id);
        return ResponseUtil.wrapOrNotFound(postCache);
    }

    /**
     * {@code DELETE  /post-caches/:id} : delete the "id" postCache.
     *
     * @param id the id of the postCache to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePostCache(@PathVariable("id") UUID id) {
        LOG.debug("REST request to delete PostCache : {}", id);
        postCacheRepository.deleteById(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }

    private <T> void updateIfPresent(Consumer<T> setter, T value) {
        if (value != null) {
            setter.accept(value);
        }
    }
}
