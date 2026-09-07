package com.minh.fakebook.post.service;

import com.minh.fakebook.post.domain.PostReaction;
import com.minh.fakebook.post.repository.PostReactionRepository;
import com.minh.fakebook.post.service.dto.PostReactionDTO;
import com.minh.fakebook.post.service.mapper.PostReactionMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.minh.fakebook.post.domain.PostReaction}.
 */
@Service
@Transactional
public class PostReactionService {

    private static final Logger LOG = LoggerFactory.getLogger(PostReactionService.class);

    private final PostReactionRepository postReactionRepository;

    private final PostReactionMapper postReactionMapper;

    private final com.minh.fakebook.post.repository.PostRepository postRepository;

    public PostReactionService(PostReactionRepository postReactionRepository, PostReactionMapper postReactionMapper, com.minh.fakebook.post.repository.PostRepository postRepository) {
        this.postReactionRepository = postReactionRepository;
        this.postReactionMapper = postReactionMapper;
        this.postRepository = postRepository;
    }

    /**
     * Save or update a postReaction (Upsert). Enforces userId via JWT.
     *
     * @param postReactionDTO the entity to save.
     * @return the persisted entity, or null if the reaction was deleted (Unlike).
     * @throws org.springframework.security.access.AccessDeniedException if not logged in.
     */
    public PostReactionDTO save(PostReactionDTO postReactionDTO) {
            LOG.debug("Request to Upsert/Toggle PostReaction : {}",
  postReactionDTO);

            org.springframework.security.core.Authentication auth = org.
  springframework.security.core.context.SecurityContextHolder.getContext().
  getAuthentication();
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".
  equals(auth.getPrincipal())) {
                throw new org.springframework.security.access.
  AccessDeniedException("Error: You must be logged in to react.");
            }
            java.util.UUID currentUserId = java.util.UUID.fromString(((org.
  springframework.security.oauth2.server.resource.authentication.
                                      JwtAuthenticationToken) auth).getToken().getSubject());
  
            // TODO (REST/FeignClient): If targetPost visibility is FRIENDS:
            // Must invoke userService API to verify if currentUserId is a friend of authorId.
            // If FALSE, immediately throw AccessDeniedException (Prevent Blind Action).

            postReactionDTO.setUserId(currentUserId);
            java.util.UUID postId = postReactionDTO.getPost().getId();
            com.minh.fakebook.post.domain.Post targetPost = postRepository.findById(postId)
                    .orElseThrow(() -> new IllegalArgumentException("Post not found"));

            if (targetPost.getStatus() == com.minh.fakebook.post.domain.enumeration.PostStatus.DELETED) {
                throw new org.springframework.security.access.AccessDeniedException("Cannot react to a deleted post.");
            }
            if (targetPost.getVisibility() == com.minh.fakebook.post.domain.enumeration.PostVisibility.PRIVATE) {
                if (!targetPost.getAuthorId().equals(currentUserId)) {
                    throw new org.springframework.security.access.AccessDeniedException(
                            "Cannot react to a private post.");
                }
            }

            java.util.Optional<com.minh.fakebook.post.domain.PostReaction>
  existingReactionOpt = postReactionRepository.findByPostIdAndUserId(postId,
  currentUserId);

            if (existingReactionOpt.isPresent()) {
                com.minh.fakebook.post.domain.PostReaction postReaction =
  existingReactionOpt.get();

                if (postReaction.getReactionType() == postReactionDTO.
  getReactionType()) {
                    postReactionRepository.delete(postReaction);
                    return null; 
                }

                postReaction.setReactionType(postReactionDTO.getReactionType());
                postReaction.setUpdatedAt(java.time.Instant.now());
                postReaction = postReactionRepository.save(postReaction);
                return postReactionMapper.toDto(postReaction);
            } else {
                com.minh.fakebook.post.domain.PostReaction postReaction =
  postReactionMapper.toEntity(postReactionDTO);
                postReaction.setCreatedAt(java.time.Instant.now());
                postReaction = postReactionRepository.save(postReaction);
                return postReactionMapper.toDto(postReaction);
            }
        }

    /**
     * Update a postReaction.
     *
     * @param postReactionDTO the entity to save.
     * @return the persisted entity.
     */
    public PostReactionDTO update(PostReactionDTO postReactionDTO) {
        throw new UnsupportedOperationException(
                "Error: Direct update is disabled. Use the Save endpoint for Upsert/Toggle.");
    }

    /**
     * Partially update a postReaction.
     *
     * @param postReactionDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<PostReactionDTO> partialUpdate(PostReactionDTO postReactionDTO) {
        throw new UnsupportedOperationException("Error: Partial update is disabled.");
    }

    /**
     * Get one postReaction by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<PostReactionDTO> findOne(UUID id) {
        LOG.debug("Request to get PostReaction : {}", id);
        return postReactionRepository.findById(id).map(postReactionMapper::toDto);
    }

    /**
     * Delete the postReaction by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
            // alow only admin or the user who created the reaction to delete it
            PostReaction reaction = postReactionRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Not found"));
            org.springframework.security.core.Authentication auth = org.springframework.security.core.context.
  SecurityContextHolder.getContext().getAuthentication();
            boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(com.minh.fakebook.post.
  security.AuthoritiesConstants.ADMIN));
            String sub = ((org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken) auth).
  getToken().getSubject();

            if (!isAdmin && !reaction.getUserId().toString().equals(sub)) {
                throw new org.springframework.security.access.AccessDeniedException("Error: You can only delete your own reaction.");
            }
            postReactionRepository.deleteById(id);
        }
}
