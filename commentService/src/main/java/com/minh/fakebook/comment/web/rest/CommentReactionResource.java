package com.minh.fakebook.comment.web.rest;

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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.minh.fakebook.comment.repository.CommentReactionRepository;
import com.minh.fakebook.comment.service.CommentReactionQueryService;
import com.minh.fakebook.comment.service.CommentReactionService;
import com.minh.fakebook.comment.service.criteria.CommentReactionCriteria;
import com.minh.fakebook.comment.service.dto.CommentReactionDTO;

import tech.jhipster.web.util.PaginationUtil;
import tech.jhipster.web.util.ResponseUtil;
import org.springframework.web.bind.annotation.PostMapping;
import com.minh.fakebook.comment.domain.enumeration.ReactionType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * REST controller for managing {@link com.minh.fakebook.comment.domain.CommentReaction}.
 */
@RestController
@RequestMapping("/api/comment-reactions")
public class CommentReactionResource {

    private static final Logger LOG = LoggerFactory.getLogger(CommentReactionResource.class);

    private static final String ENTITY_NAME = "commentServiceCommentReaction";

    @Value("${jhipster.clientApp.name:commentService}")
    private String applicationName;

    private final CommentReactionService commentReactionService;

    private final CommentReactionRepository commentReactionRepository;

    private final CommentReactionQueryService commentReactionQueryService;

    public CommentReactionResource(
        CommentReactionService commentReactionService,
        CommentReactionRepository commentReactionRepository,
        CommentReactionQueryService commentReactionQueryService
    ) {
        this.commentReactionService = commentReactionService;
        this.commentReactionRepository = commentReactionRepository;
        this.commentReactionQueryService = commentReactionQueryService;
    }




    /**
     * {@code GET  /comment-reactions} : get all the Comment Reactions.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Comment Reactions in body.
     */
    @GetMapping("")
    public ResponseEntity<List<CommentReactionDTO>> getAllCommentReactions(
        CommentReactionCriteria criteria,
        @ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get CommentReactions by criteria: {}", criteria);

        Page<CommentReactionDTO> page = commentReactionQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /comment-reactions/count} : count all the commentReactions.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countCommentReactions(CommentReactionCriteria criteria) {
        LOG.debug("REST request to count CommentReactions by criteria: {}", criteria);
        return ResponseEntity.ok().body(commentReactionQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /comment-reactions/:id} : get the "id" commentReaction.
     *
     * @param id the id of the commentReactionDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the commentReactionDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<CommentReactionDTO> getCommentReaction(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get CommentReaction : {}", id);
        Optional<CommentReactionDTO> commentReactionDTO = commentReactionService.findOne(id);
        return ResponseUtil.wrapOrNotFound(commentReactionDTO);
    }

    /**
    * {@code POST  /comment-reactions/toggle} : Toggle a reaction on a comment.
    *
    * @param commentId the ID of the target comment.
    * @param reactionType the type of reaction to apply.
    * @param jwt the JSON Web Token of the currently authenticated user.
    * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the reaction DTO, or {@code 204 (NO CONTENT)} if the reaction was removed.
    */
    @PostMapping("/toggle")
    public ResponseEntity<CommentReactionDTO> toggleReaction(
            @RequestParam(name = "commentId") UUID commentId,
            @RequestParam(name = "reactionType") ReactionType reactionType,
            @AuthenticationPrincipal Jwt jwt) {
        LOG.debug("REST request to toggle Reaction on Comment : {}", commentId);

        UUID currentUserId = UUID.fromString(jwt.getSubject());
        Optional<CommentReactionDTO> result = commentReactionService.toggleReaction(commentId,
                currentUserId, reactionType);

        if (result.isPresent()) {
            return ResponseEntity.ok(result.orElseThrow());
        } else {
            return ResponseEntity.noContent().build();
        }
    }
}
