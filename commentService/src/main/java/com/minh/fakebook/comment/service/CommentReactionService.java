package com.minh.fakebook.comment.service;

import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.minh.fakebook.comment.domain.CommentReaction;
import com.minh.fakebook.comment.repository.CommentReactionRepository;
import com.minh.fakebook.comment.service.dto.CommentReactionDTO;
import com.minh.fakebook.comment.service.mapper.CommentReactionMapper;

import com.minh.fakebook.comment.domain.Comment;
import com.minh.fakebook.comment.domain.enumeration.ReactionType;
import java.time.Instant;
import java.util.Optional;

/**
 * Service Implementation for managing {@link com.minh.fakebook.comment.domain.CommentReaction}.
 */
@Service
@Transactional
public class CommentReactionService {

    private static final Logger LOG = LoggerFactory.getLogger(CommentReactionService.class);

    private final CommentReactionRepository commentReactionRepository;

    private final CommentReactionMapper commentReactionMapper;

    public CommentReactionService(CommentReactionRepository commentReactionRepository, CommentReactionMapper commentReactionMapper) {
        this.commentReactionRepository = commentReactionRepository;
        this.commentReactionMapper = commentReactionMapper;
    }

    /**
     * Save a commentReaction.
     *
     * @param commentReactionDTO the entity to save.
     * @return the persisted entity.
     */
    public CommentReactionDTO save(CommentReactionDTO commentReactionDTO) {
        LOG.debug("Request to save CommentReaction : {}", commentReactionDTO);
        CommentReaction commentReaction = commentReactionMapper.toEntity(commentReactionDTO);
        commentReaction = commentReactionRepository.save(commentReaction);
        return commentReactionMapper.toDto(commentReaction);
    }

    /**
     * Update a commentReaction.
     *
     * @param commentReactionDTO the entity to save.
     * @return the persisted entity.
     */
    public CommentReactionDTO update(CommentReactionDTO commentReactionDTO) {
        LOG.debug("Request to update CommentReaction : {}", commentReactionDTO);
        CommentReaction commentReaction = commentReactionMapper.toEntity(commentReactionDTO);
        commentReaction = commentReactionRepository.save(commentReaction);
        return commentReactionMapper.toDto(commentReaction);
    }

    /**
     * Partially update a commentReaction.
     *
     * @param commentReactionDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<CommentReactionDTO> partialUpdate(CommentReactionDTO commentReactionDTO) {
        LOG.debug("Request to partially update CommentReaction : {}", commentReactionDTO);

        return commentReactionRepository
            .findById(commentReactionDTO.getId())
            .map(existingCommentReaction -> {
                commentReactionMapper.partialUpdate(existingCommentReaction, commentReactionDTO);

                return existingCommentReaction;
            })
            .map(commentReactionRepository::save)
            .map(commentReactionMapper::toDto);
    }

    /**
     * Get one commentReaction by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<CommentReactionDTO> findOne(UUID id) {
        LOG.debug("Request to get CommentReaction : {}", id);
        return commentReactionRepository.findById(id).map(commentReactionMapper::toDto);
    }

    /**
     * Delete the commentReaction by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete CommentReaction : {}", id);
        commentReactionRepository.deleteById(id);
    }

    /**
    * Toggles a reaction on a comment.
    *
    * @param commentId the ID of the target comment.
    * @param userId the ID of the user performing the reaction.
    * @param reactionType the selected type of reaction.
    * @return the updated or created CommentReactionDTO, or empty if the reaction was removed.
    */

    public Optional<CommentReactionDTO> toggleReaction(UUID commentId, UUID userId, ReactionType reactionType) {
        LOG.debug("Request to toggle Reaction : {} on Comment : {} by user {}", reactionType, commentId,
                userId);

        Optional<CommentReaction> existingReaction = commentReactionRepository.findByCommentIdAndUserId(commentId,
                userId);

        if (existingReaction.isPresent()) {
            CommentReaction reaction = existingReaction.orElseThrow();
            if (reaction.getReactionType().equals(reactionType)) {
                commentReactionRepository.delete(reaction);
                return Optional.empty();
            } else {
                reaction.setReactionType(reactionType);
                reaction.setUpdatedAt(Instant.now());
                return Optional.of(commentReactionMapper.toDto(commentReactionRepository.save(reaction)));
            }
        } else {
            CommentReaction newReaction = new CommentReaction();
            newReaction.setUserId(userId);
            newReaction.setReactionType(reactionType);
            newReaction.setCreatedAt(Instant.now());

            Comment commentRef = new Comment();
            commentRef.setId(commentId);
            newReaction.setComment(commentRef);

            return Optional.of(commentReactionMapper.toDto(commentReactionRepository.save(newReaction)));
        }
    }
}
