package com.minh.fakebook.comment.service;
import com.minh.fakebook.comment.web.rest.errors.BadRequestAlertException;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.minh.fakebook.comment.domain.Comment;
import com.minh.fakebook.comment.domain.enumeration.CommentStatus;
import com.minh.fakebook.comment.repository.CommentRepository;
import com.minh.fakebook.comment.service.dto.CommentDTO;
import com.minh.fakebook.comment.service.dto.CreateCommentRequestDTO;
import com.minh.fakebook.comment.service.dto.ReplyCommentRequestDTO;
import com.minh.fakebook.comment.service.mapper.CommentMapper;
import com.minh.fakebook.comment.client.UserFeignClient;
import com.minh.fakebook.comment.domain.PostCache;
import com.minh.fakebook.comment.repository.PostCacheRepository;
import com.minh.fakebook.comment.client.PostFeignClient;

/**
 * Service Implementation for managing {@link com.minh.fakebook.comment.domain.Comment}.
 */
@Service
@Transactional
public class CommentService {

    private static final Logger LOG = LoggerFactory.getLogger(CommentService.class);

    private final CommentRepository commentRepository;

    private final CommentMapper commentMapper;

    private final PostCacheRepository postCacheRepository;

    private final UserFeignClient userFeignClient;

    private final PostFeignClient postFeignClient;

    private final JdbcTemplate jdbcTemplate;

    public CommentService(
            CommentRepository commentRepository,
            CommentMapper commentMapper,
            PostCacheRepository postCacheRepository,
            UserFeignClient userFeignClient,
            PostFeignClient postFeignClient,
            JdbcTemplate jdbcTemplate 
    ) {
        this.commentRepository = commentRepository;
        this.commentMapper = commentMapper;
        this.postCacheRepository = postCacheRepository;
        this.userFeignClient = userFeignClient;
        this.postFeignClient = postFeignClient;
        this.jdbcTemplate = jdbcTemplate; 
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

        // 1. Check if post exist
        PostCache postCache = postCacheRepository.findById(request.postId())
                .orElseGet(() -> {
                    try {
                        PostFeignClient.PostSyncDTO postDTO = postFeignClient.getPostById(request.postId());
                        PostCache newCache = new PostCache();
                        newCache.setId(postDTO.id());
                        newCache.setAuthorId(postDTO.authorId());
                        newCache.setStatus(postDTO.status());
                        newCache.setVisibility(postDTO.visibility());
                        String sql = "INSERT INTO post_cache (id, author_id, status, visibility) VALUES (?, ?, ?, ?) " + "ON DUPLICATE KEY UPDATE author_id = ?, status = ?, visibility = ?";
                        jdbcTemplate.update(sql,
                            newCache.getId().toString(), newCache.getAuthorId().toString(), newCache.getStatus(), newCache.getVisibility(),
                            newCache.getAuthorId().toString(), newCache.getStatus(), newCache.getVisibility()
                        );
                        return newCache;
                    } catch (Exception e) {
                        throw new RuntimeException("FeignClient Error Detail: ", e);
                    }
                });

        // 2. Row-Level Security
        boolean isOwner = authorId.equals(postCache.getAuthorId());
        if (!isOwner) {
            if ("PRIVATE".equalsIgnoreCase(postCache.getVisibility())) {
                throw new AccessDeniedException("You do not have permission to comment on this private post.");
            } else if ("FRIENDS".equalsIgnoreCase(postCache.getVisibility())) {
                //check friend status via userService
                boolean areFriends = userFeignClient.checkFriendship(authorId, postCache.getAuthorId());
                if (!areFriends) {
                    throw new AccessDeniedException("You must be friends with the author to comment on this post.");
                }
            }
        }

        Comment comment = new Comment();
        comment.setPostId(request.postId());
        comment.setAuthorId(authorId);
        comment.setContent(request.content());
        comment.setStatus(CommentStatus.ACTIVE);

        comment = commentRepository.save(comment);
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

            return commentRepository.findById(commentId).map(comment -> {
                // Check ownership to prevent unauthorized updates
                if (!comment.getAuthorId().equals(authorId)) {
                    throw new AccessDeniedException("You can only edit your own comments.");
                }
                comment.setContent(content);
                return commentMapper.toDto(commentRepository.save(comment));
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
                commentMapper.partialUpdate(existingComment, commentDTO);

                return existingComment;
            })
            .map(commentRepository::save)
            .map(commentMapper::toDto);
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
        return commentRepository.findById(id).map(commentMapper::toDto);
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

        commentRepository.findById(commentId).ifPresent(comment -> {
            // Check ownership to prevent unauthorized deletes, but allow admins to bypass
            if (!comment.getAuthorId().equals(currentUserId) && !isAdmin) {
                throw new AccessDeniedException(
                        "You can only delete your own comments.");
            }
            comment.setStatus(CommentStatus.DELETED);
            commentRepository.save(comment);
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

            if (CommentStatus.DELETED.equals(parent.
  getStatus())) {
                throw new BadRequestAlertException("Cannot reply to a deleted comment", "comment", "parentdeleted");
            }

            Comment reply = new Comment();
            reply.setPostId(parent.getPostId());
            reply.setAuthorId(authorId);
            reply.setContent(request.content());
            reply.setStatus(CommentStatus.ACTIVE);
            reply.setParentComment(parent);

            return commentMapper.toDto(commentRepository.save(reply));
        }
}
