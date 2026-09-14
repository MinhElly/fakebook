package com.minh.fakebook.comment.web.rest;

import com.minh.fakebook.comment.repository.CommentRepository;
import com.minh.fakebook.comment.service.CommentQueryService;
import com.minh.fakebook.comment.service.CommentService;
import com.minh.fakebook.comment.service.criteria.CommentCriteria;
import com.minh.fakebook.comment.service.dto.CommentDTO;
import com.minh.fakebook.comment.web.rest.errors.BadRequestAlertException;
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
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

/**
 * REST controller for managing {@link com.minh.fakebook.comment.domain.Comment}.
 */
@RestController
@RequestMapping("/api/comments")
public class CommentResource {

    private static final Logger LOG = LoggerFactory.getLogger(CommentResource.class);

    private static final String ENTITY_NAME = "commentServiceComment";

    @Value("${jhipster.clientApp.name:commentService}")
    private String applicationName;

    private final CommentService commentService;

    private final CommentRepository commentRepository;

    private final CommentQueryService commentQueryService;

    public CommentResource(CommentService commentService, CommentRepository commentRepository,
            CommentQueryService commentQueryService) {
        this.commentService = commentService;
        this.commentRepository = commentRepository;
        this.commentQueryService = commentQueryService;
    }

    @PostMapping("/create")
        public ResponseEntity<CommentDTO> createNewComment(
            @Valid @RequestBody com.minh.fakebook.comment.service.dto.CreateCommentRequestDTO request,
            @AuthenticationPrincipal org.springframework.security.oauth2.jwt.Jwt jwt
        ) throws java.net.URISyntaxException {
            LOG.debug("REST request to create a new Comment : {}", request);
            java.util.UUID authorId = java.util.UUID.fromString(jwt.getSubject()); 
            CommentDTO result = commentService.createComment(request, authorId);

            return ResponseEntity.created(new java.net.URI("/api/comments/" + result.getId()))
                .headers(tech.jhipster.web.util.HeaderUtil.createEntityCreationAlert(applicationName, true,
  ENTITY_NAME, result.getId().toString()))
                .body(result);
        }
    /**
     * {@code POST  /comments} : Create a new comment.
     *
     * @param commentDTO the commentDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new commentDTO, or with status {@code 400 (Bad Request)} if the comment has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<CommentDTO> createComment(@Valid @RequestBody CommentDTO commentDTO) throws URISyntaxException {
        LOG.debug("REST request to save Comment : {}", commentDTO);
        if (commentDTO.getId() != null) {
            throw new BadRequestAlertException("A new comment cannot already have an ID", ENTITY_NAME, "idexists");
        }
        commentDTO = commentService.save(commentDTO);
        return ResponseEntity.created(new URI("/api/comments/" + commentDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, commentDTO.getId().toString()))
            .body(commentDTO);
    }

    /**
     * {@code PUT  /comments/:commentId} : Updates an existing comment.
     *
     * @param commentId     the unique identifier of the comment to update.
     * @param updateRequest the data transfer object containing the new content for the comment.
     * @param jwt           the JSON Web Token of the currently authenticated user, used to extract the user ID.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated commentDTO, or with status {@code 400 (Bad Request)} if the request body or ID is not valid.
     * @throws java.net.URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{commentId}")
    public ResponseEntity<CommentDTO> updateOwnComment(
            @PathVariable(value = "commentId", required = false) final java.util.UUID commentId,
            @jakarta.validation.Valid @RequestBody com.minh.fakebook.comment.service.dto.UpdateCommentRequestDTO updateRequest,
            @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.oauth2.jwt.Jwt jwt)
            throws java.net.URISyntaxException {
        LOG.debug("REST request to update Comment : {}", commentId);
        if (commentId == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }

        java.util.UUID currentUserId = java.util.UUID.fromString(jwt.getSubject());
        java.util.Optional<CommentDTO> result = commentService.updateComment(commentId, updateRequest.content(),
                currentUserId);

        return tech.jhipster.web.util.ResponseUtil.wrapOrNotFound(
                result,
                tech.jhipster.web.util.HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME,
                        commentId.toString()));
    }

    /**
     * {@code GET  /comments} : get all the Comments.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Comments in body.
     */
    @GetMapping("")
    public ResponseEntity<java.util.List<CommentDTO>> getAllComments(
            com.minh.fakebook.comment.service.criteria.CommentCriteria criteria,
            @org.springdoc.core.annotations.ParameterObject org.springframework.data.domain.Pageable pageable) {
        LOG.debug("REST request to get Comments by criteria: {}", criteria);

        if (!com.minh.fakebook.comment.security.SecurityUtils
                .hasCurrentUserThisAuthority(com.minh.fakebook.comment.security.AuthoritiesConstants.ADMIN)) {
            com.minh.fakebook.comment.service.criteria.CommentCriteria.CommentStatusFilter statusFilter = new com.minh.fakebook.comment.service.criteria.CommentCriteria.CommentStatusFilter();
            statusFilter.setEquals(com.minh.fakebook.comment.domain.enumeration.CommentStatus.ACTIVE);
            criteria.setStatus(statusFilter);
        }

        org.springframework.data.domain.Page<CommentDTO> page = commentQueryService.findByCriteria(criteria, pageable);
        org.springframework.http.HttpHeaders headers = tech.jhipster.web.util.PaginationUtil
                .generatePaginationHttpHeaders(
                        org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /comments/count} : count all the comments.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countComments(CommentCriteria criteria) {
        LOG.debug("REST request to count Comments by criteria: {}", criteria);
        return ResponseEntity.ok().body(commentQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /comments/:id} : get the "id" comment.
     *
     * @param id the id of the commentDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the commentDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<CommentDTO> getComment(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get Comment : {}", id);
        Optional<CommentDTO> commentDTO = commentService.findOne(id);
        return ResponseUtil.wrapOrNotFound(commentDTO);
    }

    /**
     * {@code DELETE  /comments/:id} : delete the "id" comment.
     *
     * @param id the id of the commentDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteComment(@PathVariable("id") UUID id) {
        LOG.debug("REST request to delete Comment : {}", id);
        commentService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
