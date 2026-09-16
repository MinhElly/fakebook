package com.minh.fakebook.comment.web.rest;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import com.minh.fakebook.comment.domain.enumeration.CommentStatus;
import com.minh.fakebook.comment.repository.CommentRepository;
import com.minh.fakebook.comment.security.AuthoritiesConstants;
import com.minh.fakebook.comment.security.SecurityUtils;
import com.minh.fakebook.comment.service.CommentQueryService;
import com.minh.fakebook.comment.service.CommentService;
import com.minh.fakebook.comment.service.criteria.CommentCriteria;
import com.minh.fakebook.comment.service.dto.CommentDTO;
import com.minh.fakebook.comment.service.dto.CreateCommentRequestDTO;
import com.minh.fakebook.comment.service.dto.ReplyCommentRequestDTO;
import com.minh.fakebook.comment.service.dto.UpdateCommentRequestDTO;
import com.minh.fakebook.comment.web.rest.errors.BadRequestAlertException;

import jakarta.validation.Valid;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.PaginationUtil;
import tech.jhipster.web.util.ResponseUtil;


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
            @Valid @RequestBody CreateCommentRequestDTO request,
            @AuthenticationPrincipal Jwt jwt
        ) throws URISyntaxException {
            LOG.debug("REST request to create a new Comment : {}", request);
            UUID authorId = UUID.fromString(jwt.getSubject()); 
            CommentDTO result = commentService.createComment(request, authorId);

            return ResponseEntity.created(new URI("/api/comments/" + result.getId()))
                .headers(HeaderUtil.createEntityCreationAlert(applicationName, true,
  ENTITY_NAME, result.getId().toString()))
                .body(result);
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
            @PathVariable(value = "commentId", required = false) final UUID commentId,
            @Valid @RequestBody UpdateCommentRequestDTO updateRequest,
            @AuthenticationPrincipal Jwt jwt)
            throws URISyntaxException {
        LOG.debug("REST request to update Comment : {}", commentId);
        if (commentId == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }

        UUID currentUserId = UUID.fromString(jwt.getSubject());
        Optional<CommentDTO> result = commentService.updateComment(commentId, updateRequest.content(),
                currentUserId);

        return ResponseUtil.wrapOrNotFound(
                result,
                HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME,
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
    public ResponseEntity<List<CommentDTO>> getAllComments(
            CommentCriteria criteria,
            @ParameterObject Pageable pageable) {
        LOG.debug("REST request to get Comments by criteria: {}", criteria);

        if (!SecurityUtils
                .hasCurrentUserThisAuthority(AuthoritiesConstants.ADMIN)) {
            CommentCriteria.CommentStatusFilter statusFilter = new CommentCriteria.CommentStatusFilter();
            statusFilter.setEquals(CommentStatus.ACTIVE);
            criteria.setStatus(statusFilter);
        }

        Page<CommentDTO> page = commentQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil
                .generatePaginationHttpHeaders(
                        ServletUriComponentsBuilder.fromCurrentRequest(), page);
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
    * {@code DELETE  /comments/:commentId} : Soft deletes an existing comment.
    *
    * @param commentId the unique identifier of the comment to delete.
    * @param jwt the JSON Web Token of the currently authenticated user, used to extract ID and roles.
    * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
    */
    @DeleteMapping("/{commentId}")
        public ResponseEntity<Void> deleteOwnComment(
            @PathVariable(value = "commentId") final UUID commentId,
            @AuthenticationPrincipal Jwt jwt
        ) {
            LOG.debug("REST request to delete Comment : {}", commentId);

            UUID currentUserId = UUID.fromString(jwt.getSubject());
            boolean isAdmin = SecurityUtils
                    .hasCurrentUserThisAuthority(AuthoritiesConstants.ADMIN);

            commentService.deleteComment(commentId, currentUserId, isAdmin);

            return ResponseEntity.noContent()
                    .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true,
                            ENTITY_NAME, commentId.toString()))
                    .build();
        }
        
    /**
    * {@code POST  /comments/reply} : Replies to an existing comment.
    *
    * @param request the data transfer object containing the parent comment ID and reply content.
    * @param jwt the JSON Web Token of the currently authenticated user.
    * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new reply commentDTO.
    * @throws java.net.URISyntaxException if the Location URI syntax is incorrect.
    */

    @PostMapping("/reply")
    public ResponseEntity<CommentDTO> replyToComment(
            @Valid @RequestBody ReplyCommentRequestDTO request,
            @AuthenticationPrincipal Jwt jwt)
            throws URISyntaxException {
        LOG.debug("REST request to reply to Comment : {}", request);

        UUID currentUserId = UUID.fromString(jwt.getSubject());
        CommentDTO result = commentService.replyToComment(request, currentUserId);

        return ResponseEntity.created(new URI("/api/comments/" + result.getId()))
                .headers(HeaderUtil.createEntityCreationAlert(applicationName, true,
                        ENTITY_NAME, result.getId().toString()))
                .body(result);
    }
}
