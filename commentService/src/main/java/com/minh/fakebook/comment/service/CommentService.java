package com.minh.fakebook.comment.service;
import com.minh.fakebook.comment.security.AuthoritiesConstants;
import com.minh.fakebook.comment.web.rest.errors.BadRequestAlertException;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.minh.fakebook.comment.domain.Comment;
import com.minh.fakebook.comment.domain.enumeration.CommentStatus;
import com.minh.fakebook.comment.repository.CommentRepository;
import com.minh.fakebook.comment.service.dto.CommentDTO;
import com.minh.fakebook.comment.service.dto.CreateCommentRequestDTO;
import com.minh.fakebook.comment.service.dto.ReplyCommentRequestDTO;
import com.minh.fakebook.comment.service.mapper.CommentMapper;
import com.minh.fakebook.comment.client.UserServiceClient;
import com.minh.fakebook.comment.domain.PostCache;
import io.namastack.outbox.Outbox;
import com.minh.fakebook.comment.service.event.EventEnvelope;
import com.minh.fakebook.comment.service.event.CommentChangedEvent;
import com.minh.fakebook.comment.service.event.CommentSummaryChanged;
import java.time.Instant;
import java.util.Map;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Service Implementation for managing {@link com.minh.fakebook.comment.domain.Comment}.
 */
@Service
@Transactional
public class CommentService {

    private static final Logger LOG = LoggerFactory.getLogger(CommentService.class);

    private final CommentRepository commentRepository;

    private final CommentMapper commentMapper;

    private final UserServiceClient userFeignClient;

    private final PostCacheResolver postCacheResolver;
    private final CommentViewAuthorizationService authorization;

    private final CommentHistoryService history;

    private final Outbox outbox;
    private final ApplicationEventPublisher eventPublisher;

    public CommentService(
            CommentRepository commentRepository,
            CommentMapper commentMapper,
            UserServiceClient userFeignClient,
            PostCacheResolver postCacheResolver,
            Outbox outbox,
            CommentViewAuthorizationService authorization,
            ApplicationEventPublisher eventPublisher,
            CommentHistoryService history
    ) {
        this.history = history;
        this.commentRepository = commentRepository;
        this.commentMapper = commentMapper;
        this.userFeignClient = userFeignClient;
        this.postCacheResolver = postCacheResolver;
        this.outbox = outbox;
        this.authorization = authorization;
        this.eventPublisher = eventPublisher;
    }


    /**
     * Create a new comment with Row-Level Security check.
     *
     * @param request  the request containing post ID and content.
     * @param authorId the ID of the author (from JWT).
     * @return the persisted comment DTO.
     * @throws IllegalArgumentException if the post does not exist in cache.
     * @throws AccessDeniedException if the user is not allowed to comment based on visibility.
     */
    public CommentDTO createComment(CreateCommentRequestDTO request, UUID authorId) {
        LOG.debug("Request to create Comment for Post {} by Author {}", request.postId(), authorId);

        validatePostAccess(request.postId(), authorId);

        Comment comment = new Comment();
        comment.setPostId(request.postId());
        comment.setAuthorId(authorId);
        comment.setContent(request.content());
        comment.setStatus(CommentStatus.ACTIVE);

        comment = commentRepository.save(comment);
        eventPublisher.publishEvent(new CommentSummaryChanged(comment.getPostId()));
        publishCommentChangedEvent(comment.getPostId(), "CREATED");
        return commentMapper.toDto(comment);
    }


    /**
     * Updates the content of an existing comment.
     *
     * @param commentId the unique identifier (UUID) of the target comment to be updated.
     * @param content   the new text content that will replace the old content of the comment.
     * @param authorId  the unique identifier (UUID) of the user who is attempting to perform this update.
     * @return an Optional containing the updated CommentDTO if the update is successful.
     * @throws org.springframework.security.access.AccessDeniedException if the user the user attempting to updatethe user attempting to update is not the original author of the comment.
     */
         public Optional<CommentDTO> updateComment(UUID commentId, String content, UUID authorId) {
            LOG.debug("Request to update Comment : {} by user {}", commentId, authorId);

            return commentRepository.findByIdForUpdate(commentId).map(comment -> {
                // Check ownership to prevent unauthorized updates
                if (!comment.getAuthorId().equals(authorId) && !isAdmin()) {
                    throw new AccessDeniedException("You can only edit your own comments.");
                }
                if (CommentStatus.DELETED.equals(comment.getStatus())) {
                    throw new BadRequestAlertException("Cannot edit a deleted comment", "comment", "commentdeleted");
                }
                if (java.util.Objects.equals(comment.getContent(), content)) return commentMapper.toDto(comment);
                if (comment.getEditCount() == 0) history.record(comment, comment.getAuthorId(), comment.getCreatedAt());
                Instant editedAt = Instant.now();
                comment.setContent(content);
                comment.setEditedAt(editedAt);
                comment.setEditCount(comment.getEditCount() + 1);
                comment = commentRepository.saveAndFlush(comment);
                history.record(comment, authorId, editedAt);
                eventPublisher.publishEvent(new CommentSummaryChanged(comment.getPostId()));
                publishCommentChangedEvent(comment.getPostId(), "UPDATED");
                return commentMapper.toDto(comment);
            });
        }


    /**
     * Save a comment.
     *
     * @param commentDTO the entity to save.
     * @return the persisted entity.
     */
    public CommentDTO save(CommentDTO commentDTO) {
        LOG.debug("Request to save Comment : {}", commentDTO);
        Comment comment = commentMapper.toEntity(commentDTO);
        comment = commentRepository.save(comment);
        eventPublisher.publishEvent(new CommentSummaryChanged(comment.getPostId()));
        return commentMapper.toDto(comment);
    }

    /**
     * Update a comment.
     *
     * @param commentDTO the entity to save.
     * @return the persisted entity.
     */
    public CommentDTO update(CommentDTO commentDTO) {
        LOG.debug("Request to update Comment : {}", commentDTO);
        Comment comment = commentMapper.toEntity(commentDTO);
        comment = commentRepository.save(comment);
        eventPublisher.publishEvent(new CommentSummaryChanged(comment.getPostId()));
        return commentMapper.toDto(comment);
    }

    /**
     * Partially update a comment.
     *
     * @param commentDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<CommentDTO> partialUpdate(CommentDTO commentDTO) {
        LOG.debug("Request to partially update Comment : {}", commentDTO);

        return commentRepository
            .findById(commentDTO.getId())
            .map(existingComment -> {
                UUID oldPostId = existingComment.getPostId();
                commentMapper.partialUpdate(existingComment, commentDTO);
                Comment saved = commentRepository.save(existingComment);
                eventPublisher.publishEvent(new CommentSummaryChanged(saved.getPostId()));
                if (oldPostId != null && !oldPostId.equals(saved.getPostId())) {
                    eventPublisher.publishEvent(new CommentSummaryChanged(oldPostId));
                }
                return commentMapper.toDto(saved);
            });
    }

    /**
     * Get one comment by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<CommentDTO> findOne(UUID id) {
        LOG.debug("Request to get Comment : {}", id);
        return commentRepository.findById(id).map(comment -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            UUID currentUserId = null;
            boolean isAdmin = false;
            if (auth != null) {
                isAdmin = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals(AuthoritiesConstants.ADMIN));
                if (auth instanceof JwtAuthenticationToken jwt) {
                    currentUserId = UUID.fromString(jwt.getToken().getSubject());
                }
            }
            if (CommentStatus.DELETED.equals(comment.getStatus()) && !isAdmin && (currentUserId == null || !currentUserId.equals(comment.getAuthorId()))) {
                throw new AccessDeniedException("You do not have permission to view this deleted comment.");
            }
            validatePostAccess(comment.getPostId(), currentUserId);
            return commentMapper.toDto(comment);
        });
    }

    /**
    * Soft deletes the comment by ID (changes status to DELETED).
    * Only the author or an administrator can perform this action.
    *
    * @param commentId the unique identifier of the target comment to delete.
    * @param currentUserId the unique identifier of the user requesting the deletion.
    * @param isAdmin a boolean flag indicating if the requesting user has the ADMIN authority.
    * @throws org.springframework.security.access.AccessDeniedException if the user is neither the
  author nor an admin.
    */
    public void deleteComment(UUID commentId, UUID currentUserId, boolean isAdmin) {
        LOG.debug("Request to delete Comment : {} by user {}", commentId, currentUserId);

        commentRepository.findByIdForUpdate(commentId).ifPresent(comment -> {
            // Check ownership to prevent unauthorized deletes, but allow admins to bypass
            if (!comment.getAuthorId().equals(currentUserId) && !isAdmin) {
                throw new AccessDeniedException(
                        "You can only delete your own comments.");
            }
            comment.setStatus(CommentStatus.DELETED);
            commentRepository.save(comment);
            eventPublisher.publishEvent(new CommentSummaryChanged(comment.getPostId()));
            publishCommentChangedEvent(comment.getPostId(), "DELETED");
        });
    }


    /**
    * Creates a reply to an existing comment.
    *
    * @param request the data transfer object containing the parent comment ID and reply content.
    * @param authorId the unique identifier of the user creating the reply.
    * @return the created reply CommentDTO.
    * @throws com.minh.fakebook.comment.web.rest.errors.BadRequestAlertException if the parent comment is not found or is DELETED.
    */
    public CommentDTO replyToComment(ReplyCommentRequestDTO request,
  UUID authorId) {
            LOG.debug("Request to reply to Comment : {} by user {}", request.parentCommentId(), authorId);

            Comment parent = commentRepository.findById(request.parentCommentId())
                .orElseThrow(() -> new BadRequestAlertException("Parent comment not found", "comment", "idnotfound"));
            validatePostAccess(parent.getPostId(), authorId);
            if (CommentStatus.DELETED.equals(parent.getStatus())) {
                throw new BadRequestAlertException("Cannot reply to a deleted comment", "comment", "parentdeleted");
            }

            Comment reply = new Comment();
            reply.setPostId(parent.getPostId());
            reply.setAuthorId(authorId);
            reply.setContent(request.content());
            reply.setStatus(CommentStatus.ACTIVE);
            reply.setParentComment(parent);
            
            reply = commentRepository.save(reply);
            eventPublisher.publishEvent(new CommentSummaryChanged(reply.getPostId()));
            publishCommentChangedEvent(reply.getPostId(), "CREATED");
            return commentMapper.toDto(reply);
        }

    @Transactional(readOnly = true)
    public Optional<java.util.List<CommentHistoryService.Revision>> history(UUID id, Long before, int size) {
        // Reuse the current read policy, including deleted-comment and post visibility checks.
        return findOne(id).map(comment -> history.list(id, before, size));
    }

    private void publishCommentChangedEvent(UUID postId, String action) {
        Instant occurredAt = Instant.now();
        EventEnvelope<CommentChangedEvent> envelope = new EventEnvelope<>(
            UUID.randomUUID(),
            "COMMENT_CHANGED",
            1,
            occurredAt,
            new CommentChangedEvent(postId, action, occurredAt)
        );
        outbox.schedule(envelope, "comment-changed-" + postId, Map.of("destination", "comment-events"));
    }

    private boolean isAdmin() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(AuthoritiesConstants.ADMIN));
    }

    public void validateCommentAccess(UUID commentId, UUID userId) {
        Comment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new BadRequestAlertException("Comment not found", "comment", "idnotfound"));
        if (CommentStatus.DELETED.equals(comment.getStatus())) {
            throw new BadRequestAlertException("Cannot interact with a deleted comment", "comment", "commentdeleted");
        }
        validatePostAccess(comment.getPostId(), userId);
    }

    public void validatePostAccess(UUID postId, UUID userId) {
        authorization.verifyCanView(java.util.List.of(postId));
    }
}
