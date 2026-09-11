package com.minh.fakebook.user.web.rest;

import com.minh.fakebook.user.repository.FollowRepository;
import com.minh.fakebook.user.service.FollowQueryService;
import com.minh.fakebook.user.service.FollowService;
import com.minh.fakebook.user.service.criteria.FollowCriteria;
import com.minh.fakebook.user.service.dto.FollowDTO;
import com.minh.fakebook.user.service.dto.FriendshipDTO;
import com.minh.fakebook.user.web.rest.errors.BadRequestAlertException;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.PaginationUtil;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link com.minh.fakebook.user.domain.Follow}.
 */
@RestController
@RequestMapping("/api/follows")
public class FollowResource {

    private static final Logger LOG = LoggerFactory.getLogger(FollowResource.class);

    private static final String ENTITY_NAME = "userServiceFollow";

    @Value("${jhipster.clientApp.name:userService}")
    private String applicationName;

    private final FollowService followService;

    private final FollowRepository followRepository;

    private final FollowQueryService followQueryService;

    public FollowResource(FollowService followService, FollowRepository followRepository, FollowQueryService followQueryService) {
        this.followService = followService;
        this.followRepository = followRepository;
        this.followQueryService = followQueryService;
    }

    /**
     * {@code POST  /follows} : Create a new follow.
     *
     * @param followDTO the followDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new followDTO, or with status {@code 400 (Bad Request)} if the follow has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<FollowDTO> createFollow(@Valid @RequestBody FollowDTO followDTO) throws URISyntaxException {
        LOG.debug("REST request to save Follow : {}", followDTO);
        if (followDTO.getId() != null) {
            throw new BadRequestAlertException("A new follow cannot already have an ID", ENTITY_NAME, "idexists");
        }
        followDTO = followService.save(followDTO);
        return ResponseEntity.created(new URI("/api/follows/" + followDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, followDTO.getId().toString()))
            .body(followDTO);
    }

    /**
     * {@code PUT  /follows/:id} : Updates an existing follow.
     *
     * @param id the id of the followDTO to save.
     * @param followDTO the followDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated followDTO,
     * or with status {@code 400 (Bad Request)} if the followDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the followDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<FollowDTO> updateFollow(
        @PathVariable(value = "id", required = false) final UUID id,
        @Valid @RequestBody FollowDTO followDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update Follow : {}, {}", id, followDTO);
        if (followDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, followDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!followRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        followDTO = followService.update(followDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, followDTO.getId().toString()))
            .body(followDTO);
    }

    /**
     * {@code PATCH  /follows/:id} : Partial updates given fields of an existing follow, field will ignore if it is null
     *
     * @param id the id of the followDTO to save.
     * @param followDTO the followDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated followDTO,
     * or with status {@code 400 (Bad Request)} if the followDTO is not valid,
     * or with status {@code 404 (Not Found)} if the followDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the followDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<FollowDTO> partialUpdateFollow(
        @PathVariable(value = "id", required = false) final UUID id,
        @NotNull @RequestBody FollowDTO followDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update Follow partially : {}, {}", id, followDTO);
        if (followDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, followDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!followRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<FollowDTO> result = followService.partialUpdate(followDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, followDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /follows} : get all the Follows.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Follows in body.
     */
    @GetMapping("")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<FollowDTO>> getAllFollows(
        FollowCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get Follows by criteria: {}", criteria);

        Page<FollowDTO> page = followQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /follows/count} : count all the follows.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Long> countFollows(FollowCriteria criteria) {
        LOG.debug("REST request to count Follows by criteria: {}", criteria);
        return ResponseEntity.ok().body(followQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /follows/:id} : get the "id" follow.
     *
     * @param id the id of the followDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the followDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<FollowDTO> getFollow(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get Follow : {}", id);
        Optional<FollowDTO> followDTO = followService.findOne(id);
        return ResponseUtil.wrapOrNotFound(followDTO);
    }

    /**
     * {@code DELETE  /follows/:id} : delete the "id" follow.
     *
     * @param id the id of the followDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> deleteFollow(@PathVariable("id") UUID id) {
        LOG.debug("REST request to delete Follow : {}", id);
        followService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
    @PostMapping ("/user/{id}")
    public ResponseEntity<FollowDTO> followUser(
        @PathVariable("id") UUID targetUserId,
        @AuthenticationPrincipal Jwt jwt){
            UUID senderId = UUID.fromString(jwt.getSubject());
            FollowDTO result = followService.followUser(senderId,targetUserId);
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        }
    @DeleteMapping("/user/{id}")
    public ResponseEntity<Void> unfollowUser(
        @PathVariable("id") UUID targetUserId,
        @AuthenticationPrincipal Jwt jwt){
            UUID senderId = UUID.fromString(jwt.getSubject());
            followService.unfollowUser(senderId, targetUserId);
            return ResponseEntity.noContent().build();
    }
    @GetMapping("/me/following")
    public ResponseEntity<List<FollowDTO>> getMyFollowingList(
        @AuthenticationPrincipal Jwt jwt,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable){
            UUID userId = UUID.fromString(jwt.getSubject());
            Page<FollowDTO> page = followService.getFollowingList(userId, pageable);
            HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(),page );
            return ResponseEntity.ok().headers(headers).body(page.getContent());
    }
    @GetMapping("/me/follower")
    public ResponseEntity<List<FollowDTO>> getMyFollowerList(
        @AuthenticationPrincipal Jwt jwt,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable){
            UUID userId = UUID.fromString(jwt.getSubject());
            Page<FollowDTO> page = followService.getFollowerList(userId, pageable);
            HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(),page );
            return ResponseEntity.ok().headers(headers).body(page.getContent());
    }
    @GetMapping("/user/{userId}/following")
    public ResponseEntity<List<FollowDTO>> getUserFollowingList(
        @PathVariable("userId") UUID userId,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable){
            Page<FollowDTO> page = followService.getFollowingList(userId, pageable);
            HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(),page );
            return ResponseEntity.ok().headers(headers).body(page.getContent());
    }
    @GetMapping("/user/{userId}/follower")
    public ResponseEntity<List<FollowDTO>> getUserFollowerList(
        @PathVariable("userId") UUID userId,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable){
            Page<FollowDTO> page = followService.getFollowerList(userId, pageable);
            HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(),page );
            return ResponseEntity.ok().headers(headers).body(page.getContent());
    }
}
