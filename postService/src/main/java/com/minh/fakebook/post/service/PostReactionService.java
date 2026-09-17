package com.minh.fakebook.post.service;

import com.minh.fakebook.post.domain.Post;
import com.minh.fakebook.post.domain.PostReaction;
import com.minh.fakebook.post.domain.enumeration.PostStatus;
import com.minh.fakebook.post.domain.enumeration.PostVisibility;
import com.minh.fakebook.post.repository.PostReactionRepository;
import com.minh.fakebook.post.repository.PostRepository;
import com.minh.fakebook.post.security.AuthoritiesConstants;
import com.minh.fakebook.post.service.dto.PostReactionDTO;
import com.minh.fakebook.post.service.mapper.PostReactionMapper;
import java.time.Instant;
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

/**
 * Service Implementation for managing {@link com.minh.fakebook.post.domain.PostReaction}.
 */
@Service
@Transactional
public class PostReactionService {

    private static final Logger LOG = LoggerFactory.getLogger(PostReactionService.class);

    private final PostReactionRepository postReactionRepository;

    private final PostReactionMapper postReactionMapper;

    private final PostRepository postRepository;

    public PostReactionService(PostReactionRepository postReactionRepository, PostReactionMapper postReactionMapper, PostRepository postRepository) {
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

            Authentication auth = SecurityContextHolder.getContext().
  getAuthentication();
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".
  equals(auth.getPrincipal())) {
                throw new AccessDeniedException("Error: You must be logged in to react.");
            }
            UUID currentUserId = UUID.fromString(((JwtAuthenticationToken) auth).getToken().getSubject());
  
            // TODO (REST/FeignClient): If targetPost visibility is FRIENDS:
            // Must invoke userService API to verify if currentUserId is a friend of authorId.
            // If FALSE, immediately throw AccessDeniedException (Prevent Blind Action).

            postReactionDTO.setUserId(currentUserId);
            UUID postId = postReactionDTO.getPost().getId();
            Post targetPost = postRepository.findById(postId)
                    .orElseThrow(() -> new IllegalArgumentException("Post not found"));

            if (targetPost.getStatus() == PostStatus.DELETED) {
                throw new AccessDeniedException("Cannot react to a deleted post.");
            }
            if (targetPost.getVisibility() == PostVisibility.PRIVATE) {
                if (!targetPost.getAuthorId().equals(currentUserId)) {
                    throw new AccessDeniedException(
                            "Cannot react to a private post.");
                }
            }

            Optional<PostReaction>
  existingReactionOpt = postReactionRepository.findByPostIdAndUserId(postId,
  currentUserId);

            if (existingReactionOpt.isPresent()) {
                PostReaction postReaction =
  existingReactionOpt.orElseThrow();

                if (postReaction.getReactionType() == postReactionDTO.
  getReactionType()) {
                    postReactionRepository.delete(postReaction);
                    return null; 
                }

                postReaction.setReactionType(postReactionDTO.getReactionType());
                postReaction.setUpdatedAt(Instant.now());
                postReaction = postReactionRepository.save(postReaction);
                return postReactionMapper.toDto(postReaction);
            } else {
                PostReaction postReaction =
  postReactionMapper.toEntity(postReactionDTO);
                postReaction.setCreatedAt(Instant.now());
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
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(AuthoritiesConstants.ADMIN));
            String sub = ((JwtAuthenticationToken) auth).
  getToken().getSubject();

            if (!isAdmin && !reaction.getUserId().toString().equals(sub)) {
                throw new AccessDeniedException("Error: You can only delete your own reaction.");
            }
            postReactionRepository.deleteById(id);
        }
}
