package com.minh.fakebook.user.web.rest;

import com.minh.fakebook.user.repository.FriendRequestRepository;
import com.minh.fakebook.user.service.FriendRequestQueryService;
import com.minh.fakebook.user.service.FriendRequestService;
import com.minh.fakebook.user.service.criteria.FriendRequestCriteria;
import com.minh.fakebook.user.service.dto.FriendRequestDTO;
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
 * REST controller for managing {@link com.minh.fakebook.user.domain.FriendRequest}.
 */
@RestController
@RequestMapping("/api/friend-requests")
public class FriendRequestResource {

    private static final Logger LOG = LoggerFactory.getLogger(FriendRequestResource.class);

    private static final String ENTITY_NAME = "userServiceFriendRequest";

    @Value("${jhipster.clientApp.name:userService}")
    private String applicationName;

    private final FriendRequestService friendRequestService;

    private final FriendRequestRepository friendRequestRepository;

    private final FriendRequestQueryService friendRequestQueryService;

    public FriendRequestResource(
        FriendRequestService friendRequestService,
        FriendRequestRepository friendRequestRepository,
        FriendRequestQueryService friendRequestQueryService
    ) {
        this.friendRequestService = friendRequestService;
        this.friendRequestRepository = friendRequestRepository;
        this.friendRequestQueryService = friendRequestQueryService;
    }

    /**
     * {@code POST  /friend-requests} : Create a new friendRequest.
     *
     * @param friendRequestDTO the friendRequestDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new friendRequestDTO, or with status {@code 400 (Bad Request)} if the friendRequest has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<FriendRequestDTO> createFriendRequest(@Valid @RequestBody FriendRequestDTO friendRequestDTO)
        throws URISyntaxException {
        LOG.debug("REST request to save FriendRequest : {}", friendRequestDTO);
        if (friendRequestDTO.getId() != null) {
            throw new BadRequestAlertException("A new friendRequest cannot already have an ID", ENTITY_NAME, "idexists");
        }
        friendRequestDTO = friendRequestService.save(friendRequestDTO);
        return ResponseEntity.created(new URI("/api/friend-requests/" + friendRequestDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, friendRequestDTO.getId().toString()))
            .body(friendRequestDTO);
    }

    /**
     * {@code PUT  /friend-requests/:id} : Updates an existing friendRequest.
     *
     * @param id the id of the friendRequestDTO to save.
     * @param friendRequestDTO the friendRequestDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated friendRequestDTO,
     * or with status {@code 400 (Bad Request)} if the friendRequestDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the friendRequestDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<FriendRequestDTO> updateFriendRequest(
        @PathVariable(value = "id", required = false) final UUID id,
        @Valid @RequestBody FriendRequestDTO friendRequestDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update FriendRequest : {}, {}", id, friendRequestDTO);
        if (friendRequestDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, friendRequestDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!friendRequestRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        friendRequestDTO = friendRequestService.update(friendRequestDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, friendRequestDTO.getId().toString()))
            .body(friendRequestDTO);
    }

    /**
     * {@code PATCH  /friend-requests/:id} : Partial updates given fields of an existing friendRequest, field will ignore if it is null
     *
     * @param id the id of the friendRequestDTO to save.
     * @param friendRequestDTO the friendRequestDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated friendRequestDTO,
     * or with status {@code 400 (Bad Request)} if the friendRequestDTO is not valid,
     * or with status {@code 404 (Not Found)} if the friendRequestDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the friendRequestDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<FriendRequestDTO> partialUpdateFriendRequest(
        @PathVariable(value = "id", required = false) final UUID id,
        @NotNull @RequestBody FriendRequestDTO friendRequestDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update FriendRequest partially : {}, {}", id, friendRequestDTO);
        if (friendRequestDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, friendRequestDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!friendRequestRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<FriendRequestDTO> result = friendRequestService.partialUpdate(friendRequestDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, friendRequestDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /friend-requests} : get all the Friend Requests.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Friend Requests in body.
     */
    @GetMapping("")
    public ResponseEntity<List<FriendRequestDTO>> getAllFriendRequests(
        FriendRequestCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get FriendRequests by criteria: {}", criteria);

        Page<FriendRequestDTO> page = friendRequestQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /friend-requests/count} : count all the friendRequests.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countFriendRequests(FriendRequestCriteria criteria) {
        LOG.debug("REST request to count FriendRequests by criteria: {}", criteria);
        return ResponseEntity.ok().body(friendRequestQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /friend-requests/:id} : get the "id" friendRequest.
     *
     * @param id the id of the friendRequestDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the friendRequestDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<FriendRequestDTO> getFriendRequest(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get FriendRequest : {}", id);
        Optional<FriendRequestDTO> friendRequestDTO = friendRequestService.findOne(id);
        return ResponseUtil.wrapOrNotFound(friendRequestDTO);
    }

    /**
     * {@code DELETE  /friend-requests/:id} : delete the "id" friendRequest.
     *
     * @param id the id of the friendRequestDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFriendRequest(@PathVariable("id") UUID id) {
        LOG.debug("REST request to delete FriendRequest : {}", id);
        friendRequestService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
