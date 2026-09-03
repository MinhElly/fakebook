package com.minh.fakebook.user.web.rest;

import com.minh.fakebook.user.repository.FriendshipRepository;
import com.minh.fakebook.user.service.FriendshipQueryService;
import com.minh.fakebook.user.service.FriendshipService;
import com.minh.fakebook.user.service.criteria.FriendshipCriteria;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.PaginationUtil;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link com.minh.fakebook.user.domain.Friendship}.
 */
@RestController
@RequestMapping("/api/friendships")
public class FriendshipResource {

    private static final Logger LOG = LoggerFactory.getLogger(FriendshipResource.class);

    private static final String ENTITY_NAME = "userServiceFriendship";

    @Value("${jhipster.clientApp.name:userService}")
    private String applicationName;

    private final FriendshipService friendshipService;

    private final FriendshipRepository friendshipRepository;

    private final FriendshipQueryService friendshipQueryService;

    public FriendshipResource(
        FriendshipService friendshipService,
        FriendshipRepository friendshipRepository,
        FriendshipQueryService friendshipQueryService
    ) {
        this.friendshipService = friendshipService;
        this.friendshipRepository = friendshipRepository;
        this.friendshipQueryService = friendshipQueryService;
    }

    /**
     * {@code POST  /friendships} : Create a new friendship.
     *
     * @param friendshipDTO the friendshipDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new friendshipDTO, or with status {@code 400 (Bad Request)} if the friendship has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<FriendshipDTO> createFriendship(@Valid @RequestBody FriendshipDTO friendshipDTO) throws URISyntaxException {
        LOG.debug("REST request to save Friendship : {}", friendshipDTO);
        if (friendshipDTO.getId() != null) {
            throw new BadRequestAlertException("A new friendship cannot already have an ID", ENTITY_NAME, "idexists");
        }
        friendshipDTO = friendshipService.save(friendshipDTO);
        return ResponseEntity.created(new URI("/api/friendships/" + friendshipDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, friendshipDTO.getId().toString()))
            .body(friendshipDTO);
    }

    /**
     * {@code PUT  /friendships/:id} : Updates an existing friendship.
     *
     * @param id the id of the friendshipDTO to save.
     * @param friendshipDTO the friendshipDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated friendshipDTO,
     * or with status {@code 400 (Bad Request)} if the friendshipDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the friendshipDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<FriendshipDTO> updateFriendship(
        @PathVariable(value = "id", required = false) final UUID id,
        @Valid @RequestBody FriendshipDTO friendshipDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update Friendship : {}, {}", id, friendshipDTO);
        if (friendshipDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, friendshipDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!friendshipRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        friendshipDTO = friendshipService.update(friendshipDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, friendshipDTO.getId().toString()))
            .body(friendshipDTO);
    }

    /**
     * {@code PATCH  /friendships/:id} : Partial updates given fields of an existing friendship, field will ignore if it is null
     *
     * @param id the id of the friendshipDTO to save.
     * @param friendshipDTO the friendshipDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated friendshipDTO,
     * or with status {@code 400 (Bad Request)} if the friendshipDTO is not valid,
     * or with status {@code 404 (Not Found)} if the friendshipDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the friendshipDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<FriendshipDTO> partialUpdateFriendship(
        @PathVariable(value = "id", required = false) final UUID id,
        @NotNull @RequestBody FriendshipDTO friendshipDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update Friendship partially : {}, {}", id, friendshipDTO);
        if (friendshipDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, friendshipDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!friendshipRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<FriendshipDTO> result = friendshipService.partialUpdate(friendshipDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, friendshipDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /friendships} : get all the Friendships.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Friendships in body.
     */
    @GetMapping("")
    public ResponseEntity<List<FriendshipDTO>> getAllFriendships(
        FriendshipCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get Friendships by criteria: {}", criteria);

        Page<FriendshipDTO> page = friendshipQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /friendships/count} : count all the friendships.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countFriendships(FriendshipCriteria criteria) {
        LOG.debug("REST request to count Friendships by criteria: {}", criteria);
        return ResponseEntity.ok().body(friendshipQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /friendships/:id} : get the "id" friendship.
     *
     * @param id the id of the friendshipDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the friendshipDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<FriendshipDTO> getFriendship(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get Friendship : {}", id);
        Optional<FriendshipDTO> friendshipDTO = friendshipService.findOne(id);
        return ResponseUtil.wrapOrNotFound(friendshipDTO);
    }

    /**
     * {@code DELETE  /friendships/:id} : delete the "id" friendship.
     *
     * @param id the id of the friendshipDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFriendship(@PathVariable("id") UUID id) {
        LOG.debug("REST request to delete Friendship : {}", id);
        friendshipService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
