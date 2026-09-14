package com.minh.fakebook.comment.service;

import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.minh.fakebook.comment.domain.Comment;
import com.minh.fakebook.comment.repository.CommentRepository;
import com.minh.fakebook.comment.service.dto.CommentDTO;
import com.minh.fakebook.comment.service.mapper.CommentMapper;

/**
 * Service Implementation for managing {@link com.minh.fakebook.comment.domain.Comment}.
 */
@Service
@Transactional
public class CommentService {

    private static final Logger LOG = LoggerFactory.getLogger(CommentService.class);

    private final CommentRepository commentRepository;

    private final CommentMapper commentMapper;

    public CommentService(CommentRepository commentRepository, CommentMapper commentMapper) {
        this.commentRepository = commentRepository;
        this.commentMapper = commentMapper;
    }

    /**
     * Create a new comment.
     *
     * @param request  the request containing post ID and content.
     * @param authorId the ID of the author (from JWT).
     * @return the persisted comment DTO.
     */
    public CommentDTO createComment(com.minh.fakebook.comment.service.dto.CreateCommentRequestDTO request,
            UUID authorId) {
        LOG.debug("Request to create Comment for Post {} by Author {}", request.postId(), authorId);
        Comment comment = new Comment();
        comment.setPostId(request.postId());
        comment.setAuthorId(authorId);
        comment.setContent(request.content());
        comment.setStatus(com.minh.fakebook.comment.domain.enumeration.CommentStatus.ACTIVE);

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
         public java.util.Optional<CommentDTO> updateComment(java.util.UUID commentId, String content, java.
  util.UUID authorId) {
            LOG.debug("Request to update Comment : {} by user {}", commentId, authorId);

            return commentRepository.findById(commentId).map(comment -> {
                // Check ownership to prevent unauthorized updates
                if (!comment.getAuthorId().equals(authorId)) {
                    throw new org.springframework.security.access.AccessDeniedException("You can only edit your own comments.");
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
     * Delete the comment by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete Comment : {}", id);
        commentRepository.deleteById(id);
    }
}
