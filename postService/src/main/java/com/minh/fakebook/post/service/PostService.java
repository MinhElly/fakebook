package com.minh.fakebook.post.service;

import com.minh.fakebook.post.domain.Post;
import com.minh.fakebook.post.repository.PostRepository;
import com.minh.fakebook.post.service.dto.PostDTO;
import com.minh.fakebook.post.service.mapper.PostMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.minh.fakebook.post.domain.Post}.
 */
@Service
@Transactional
public class PostService {

    private static final Logger LOG = LoggerFactory.getLogger(PostService.class);

    private final PostRepository postRepository;

    private final PostMapper postMapper;

    private final com.minh.fakebook.post.repository.PostMediaRepository postMediaRepository;

    public PostService(PostRepository postRepository, PostMapper postMapper, com.minh.fakebook.post.repository.PostMediaRepository postMediaRepository) {
        this.postRepository = postRepository;
        this.postMapper = postMapper;
        this.postMediaRepository = postMediaRepository;
    }

    /**
     * Save a post.
     *
     * @param postDTO the entity to save.
     * @return the persisted entity.
     */
    public PostDTO save(PostDTO postDTO) {
        LOG.debug("Request to save Post : {}", postDTO);
        Post post = postMapper.toEntity(postDTO);
        post = postRepository.save(post);
        return postMapper.toDto(post);
    }

    /**
     * Update a post (content, visibility, and media). Enforces authorship.
     *
     * @param postDTO the entity to update.
     * @return the persisted entity.
     * @throws org.springframework.security.access.AccessDeniedException if not the author.
     */
    public PostDTO update(PostDTO postDTO) {
        LOG.debug("Request to update Post : {}", postDTO);

        //1. Fetch existing post from DB
        com.minh.fakebook.post.domain.Post existingPost = postRepository.findById(postDTO.getId())
                .orElseThrow(() -> new IllegalArgumentException("Error: Post not found " + postDTO.getId()));

        //2. Verify authorship
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Error: You must be logged in to update a post.");
        }
        String sub = ((org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken) auth)
                .getToken().getSubject();

        if (!existingPost.getAuthorId().toString().equals(sub)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Error: Only the author can update this post.");
        }
        
        //3. Update ONLY allowed fields
        existingPost.setContent(postDTO.getContent());
        existingPost.setVisibility(postDTO.getVisibility());
        existingPost.setUpdatedAt(java.time.Instant.now());

        postRepository.save(existingPost);

        //4. Replace media links
        postMediaRepository.deleteByPostId(existingPost.getId());
        java.util.List<java.util.UUID> newMediaIds = postDTO.getMediaIds();
        if (newMediaIds != null && !newMediaIds.isEmpty()) {
            java.util.List<com.minh.fakebook.post.domain.PostMedia> postMedias = new java.util.ArrayList<>();
            for (int i = 0; i < newMediaIds.size(); i++) {
                com.minh.fakebook.post.domain.PostMedia pm = new com.minh.fakebook.post.domain.PostMedia();
                pm.setMediaId(newMediaIds.get(i));
                pm.setPost(existingPost);
                pm.setDisplayOrder(i);
                pm.setCreatedAt(java.time.Instant.now());
                postMedias.add(pm);
            }
            postMediaRepository.saveAll(postMedias);
        }

        //5. Convert and return DTO
        PostDTO resultDTO = postMapper.toDto(existingPost);
        resultDTO.setMediaIds(newMediaIds);

        return resultDTO;
    }

    /**
     * Partially update a post.
     *
     * @param postDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<PostDTO> partialUpdate(PostDTO postDTO) {
        LOG.debug("Request to partially update Post : {}", postDTO);

        return postRepository
                .findById(postDTO.getId())
                .map(existingPost -> {
                    postMapper.partialUpdate(existingPost, postDTO);

                    return existingPost;
                })
                .map(postRepository::save)
                .map(postMapper::toDto);
    }
    
    /**
     * Delete the post by id. Enforces authorship and cleans up local links.
     *
     * @param id the id of the entity.
     * @throws org.springframework.security.access.AccessDeniedException if not the author.
     */
    public void delete(java.util.UUID id) {
        //1. Fetch existing post form DB
        com.minh.fakebook.post.domain.Post existingPost = postRepository.findById(id).
                orElseThrow(() -> new IllegalArgumentException("Error: Post not found " + id));
        
        //2. Verify authorship
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Error: You must logged in to delete a post.");
        }
        String sub = ((org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken) auth)
                .getToken().getSubject();
        if (!existingPost.getAuthorId().toString().equals(sub)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Errorr: Only the author can delete this post.");
        }
                
        //3. Clear local media links first to avoid DB Constraint violations
        postMediaRepository.deleteByPostId(id);

        //4. TODO: Namastack Outbox Event - Notify mediaService to clean up physical files via Kafka

        //5. Delete the actual post
        postRepository.deleteById(id);
    }

    /**
     * Handles the business logic for creating a new post.
     * Automatically extracts the author's UUID from the JWT token to prevent
     * spoofing attacks.
     *
     * @param content    The text content of the post.
     * @param visibility The visibility level of the post (PUBLIC, FRIENDS,
     *                   PRIVATE).
     * @return A PostDTO containing the newly created post data.
     * @throws RuntimeException if the user is not authenticated.
     */
    public com.minh.fakebook.post.service.dto.PostDTO createPost(String content,
            com.minh.fakebook.post.domain.enumeration.PostVisibility visibility,
            java.util.List<java.util.UUID> mediaIds) {
        LOG.debug("Request to create a new Post by current user");

        // 1. Extract user UUID from JWT Token 
        java.util.UUID authorId;
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        if (auth instanceof org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken jwtAuth) {
            String sub = jwtAuth.getToken().getSubject();
            authorId = java.util.UUID.fromString(sub);
        } else {
            throw new RuntimeException("Error: Cannot extract JWT token to get User ID.");
        }

        // 2. Initialize new Post entity
        com.minh.fakebook.post.domain.Post newPost = new com.minh.fakebook.post.domain.Post();
        newPost.setAuthorId(authorId);
        newPost.setContent(content);
        newPost.setVisibility(visibility);
        newPost.setStatus(com.minh.fakebook.post.domain.enumeration.PostStatus.ACTIVE);
        newPost.setCreatedAt(java.time.Instant.now());

        // 3. Save to Database
        newPost = postRepository.save(newPost);

        // 4. Save attached media files (if any)
        if (mediaIds != null && !mediaIds.isEmpty()) {
            int order = 0;
            for (java.util.UUID mediaId : mediaIds) {
                com.minh.fakebook.post.domain.PostMedia postMedia = new com.minh.fakebook.post.domain.PostMedia();
                postMedia.setPost(newPost);
                postMedia.setMediaId(mediaId);
                postMedia.setDisplayOrder(order++);
                postMedia.setCreatedAt(java.time.Instant.now());
                postMediaRepository.save(postMedia);
            }
        }

        return postMapper.toDto(newPost);
    }
    
    /**
     * Get one post by id, including its media attachments. Enforces privacy visibility rules.
     *
     * @param id the id of the entity.
     * @return the entity wrapped in Optional.
     * @throws org.springframework.security.access.AccessDeniedException if the current user lacks permission.
     */
    @Transactional(readOnly = true)
    public Optional<PostDTO> findOne(java.util.UUID id) {
        LOG.debug("Request to get Post : {}", id);
        return postRepository.findById(id).map(post -> {

            org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            boolean isGuest = (auth == null || !auth.isAuthenticated() || "annonymousUser".equals(auth.getPrincipal()));

            // 1. Check Private Visibility
            if (post.getVisibility() == com.minh.fakebook.post.domain.enumeration.PostVisibility.PRIVATE) {
                if (isGuest) {
                    throw new org.springframework.security.access.AccessDeniedException(
                            "Error: You do not have permission to view this private post.");
                }
                String sub = ((org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken) auth)
                        .getToken().getSubject();
                if (!post.getAuthorId().toString().equals(sub)) {
                    throw new org.springframework.security.access.AccessDeniedException(
                            "Error: Only the author can view this private post.");
                }
            }

            // 2. Check Friends Visibility
            if (post.getVisibility() == com.minh.fakebook.post.domain.enumeration.PostVisibility.FRIENDS) {
                if (isGuest) {
                    throw new org.springframework.security.access.AccessDeniedException(
                            "Error: You must be logged in to view this friends-only post.");
                }
                // TODO: Integrate with FriendshipService via FeignClient/Kafka to verify friendship status between the current user and the post's author.
            }

            //3. Convert to DTO
            com.minh.fakebook.post.service.dto.PostDTO dto = postMapper.toDto(post);

            //4. Fetch and attach media IDs
            java.util.List<java.util.UUID> mediaIds = postMediaRepository.findByPostIdOrderByDisplayOrderAsc(post.getId())
                    .stream()
                    .map(com.minh.fakebook.post.domain.PostMedia::getMediaId)
                    .toList();
            dto.setMediaIds(mediaIds);

            return dto;
        });
    }
}

