package com.minh.fakebook.post.service;
import com.minh.fakebook.post.repository.PostMediaRepository;
import com.minh.fakebook.post.repository.PostReactionRepository;
import com.minh.fakebook.post.repository.PostRepository;
import com.minh.fakebook.post.service.dto.PostDTO;
import com.minh.fakebook.post.service.dto.event.PostCreateEvent;
import com.minh.fakebook.post.service.mapper.PostMapper;
import java.time.Instant;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.access.AccessDeniedException;
import com.minh.fakebook.post.domain.PostMedia;
import com.minh.fakebook.post.domain.Post;
import com.minh.fakebook.post.domain.enumeration.PostStatus;
import com.minh.fakebook.post.domain.enumeration.PostVisibility;
import com.minh.fakebook.post.security.AuthoritiesConstants;

/**
 * Service Implementation for managing {@link Post}.
 */
@Service
@Transactional
public class PostService {

    private static final Logger LOG = LoggerFactory.getLogger(PostService.class);

    private final PostRepository postRepository;

    private final PostMapper postMapper;

    private final PostMediaRepository postMediaRepository;

    private final PostReactionRepository postReactionRepository;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public PostService(PostRepository postRepository, PostMapper postMapper, PostMediaRepository postMediaRepository, PostReactionRepository postReactionRepository, KafkaTemplate<String, Object> kafkaTemplate) {
        this.postRepository = postRepository;
        this.postMapper = postMapper;
        this.postMediaRepository = postMediaRepository;
        this.postReactionRepository = postReactionRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Save a post.
     *
     * @param postDTO the entity to save.
     * @return the persisted entity.
     */
    public PostDTO save(PostDTO postDTO) {
        LOG.debug("Request to save Post : {}", postDTO);

        if (postDTO.getAuthorId() == null) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                if (auth instanceof JwtAuthenticationToken jwtAuth) {
                    String sub = jwtAuth.getToken().getSubject();
                    postDTO.setAuthorId(UUID.fromString(sub));
                }
            }
        }

        if (postDTO.getAuthorId() == null) {
            throw new AccessDeniedException("Error: You must be logged in to create a post.");
        }

        if (postDTO.getStatus() == null) {
            postDTO.setStatus(PostStatus.ACTIVE);
        }

        if (postDTO.getCreatedAt() == null) {
            postDTO.setCreatedAt(Instant.now());
        }

        Post post = postMapper.toEntity(postDTO);
        Post savedPost = postRepository.save(post);
        PostCreateEvent event = new PostCreateEvent(
            savedPost.getId(),
            savedPost.getAuthorId(),
            savedPost.getVisibility(),
            savedPost.getCreatedAt()
        );
        kafkaTemplate.send("post-events", savedPost.getAuthorId().toString(), event);

        List<UUID> mediaIds = postDTO.getMediaIds();
        if (mediaIds != null && !mediaIds.isEmpty()) {
            List<PostMedia> postMedias = new ArrayList<>();
            for (int i = 0; i < mediaIds.size(); i++) {
                PostMedia pm = new PostMedia();
                pm.setMediaId(mediaIds.get(i));
                pm.setPost(savedPost);
                pm.setDisplayOrder(i);
                pm.setCreatedAt(Instant.now());
                postMedias.add(pm);
            }
            postMediaRepository.saveAll(postMedias);
        }

        PostDTO resultDTO = postMapper.toDto(savedPost);
        resultDTO.setMediaIds(mediaIds);
        return resultDTO;
    }

    /**
     * Update a post (content, visibility, and media). Enforces authorship.
     *
     * @param postDTO the entity to update.
     *                                                                   
     * @return the persisted entity.
     * @throws AccessDeniedException if not the author.
     */
    public PostDTO update(PostDTO postDTO) {
        LOG.debug("Request to update Post : {}", postDTO);

        //1. Fetch existing post from DB
        Post existingPost = postRepository.findById(postDTO.getId())
                .orElseThrow(() -> new IllegalArgumentException("Error: Post not found " + postDTO.getId()));

        //2. Verify authorship
        Authentication auth = SecurityContextHolder
                .getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new AccessDeniedException(
                    "Error: You must be logged in to update a post.");
        }
        String sub = ((JwtAuthenticationToken) auth)
                .getToken().getSubject();

        if (!existingPost.getAuthorId().toString().equals(sub)) {
            throw new AccessDeniedException(
                    "Error: Only the author can update this post.");
        }
        
        //3. Update ONLY allowed fields
        existingPost.setContent(postDTO.getContent());
        existingPost.setVisibility(postDTO.getVisibility());
        existingPost.setUpdatedAt(Instant.now());

        postRepository.save(existingPost);

        //4. Replace media links
        postMediaRepository.deleteByPostId(existingPost.getId());
        List<UUID> newMediaIds = postDTO.getMediaIds();
        if (newMediaIds != null && !newMediaIds.isEmpty()) {
            List<PostMedia> postMedias = new ArrayList<>();
            for (int i = 0; i < newMediaIds.size(); i++) {
                PostMedia pm = new PostMedia();
                pm.setMediaId(newMediaIds.get(i));
                pm.setPost(existingPost);
                pm.setDisplayOrder(i);
                pm.setCreatedAt(Instant.now());
                postMedias.add(pm);
            }
            postMediaRepository.saveAll(postMedias);
        }

        //5. Convert and return DTO
        PostDTO resultDTO = postMapper.toDto(existingPost);
        resultDTO.setMediaIds(newMediaIds);

        // TODO (Kafka/Outbox): Emit "POST_UPDATED" event to Kafka.
        // Purpose: Notify feedService to update the post content in the News Feeds.

        return resultDTO;
    }

    /**
     * Partially update a post. Enforces authorship and prevents Mass Assignment.
     *
     * @param postDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<PostDTO> partialUpdate(PostDTO postDTO) {
        LOG.debug("Request to partially update Post : {}", postDTO);

        return postRepository
                .findById(postDTO.getId())
                .map(existingPost -> {
                    Authentication auth = SecurityContextHolder
                            .getContext().getAuthentication();
                    if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
                        throw new AccessDeniedException(
                                "Error: You must be logged in to update a post.");
                    }
                    String sub = ((JwtAuthenticationToken) auth)
                            .getToken().getSubject();

                    if (!existingPost.getAuthorId().toString().equals(sub)) {
                        throw new AccessDeniedException(
                                "Error: Only the author can update this post.");
                    }

                    // update only allowed fields (content, visibility). Do NOT update sensitive fields like status, authorId, or id.
                    if (postDTO.getContent() != null) {
                        existingPost.setContent(postDTO.getContent());
                    }
                    if (postDTO.getVisibility() != null) {
                        existingPost.setVisibility(postDTO.getVisibility());
                    }

                    existingPost.setUpdatedAt(Instant.now());

                    return existingPost;
                })
                .map(postRepository::save)
                .map(postMapper::toDto);

            // TODO (Kafka/Outbox): Emit "POST_UPDATED" event to Kafka.
            // Purpose: Notify feedService to update the post content in the News Feeds.
    }
    
    /**
     * Delete the post by id. Enforces authorship or ADMIN role, and cleans up local links.
     *
     * @param id the id of the entity.
     * @throws AccessDeniedException if not the author or admin.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete Post : {}", id);
        //1. Fetch existing post form DB
        Post existingPost = postRepository.findById(id).
                orElseThrow(() -> new IllegalArgumentException("Error: Post not found " + id));
        
        //2. Verify authorship or ADMIN role
        Authentication auth = SecurityContextHolder
                .getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new AccessDeniedException(
                    "Error: You must be logged in to delete a post.");
        }

        String sub = ((JwtAuthenticationToken) auth)
                .getToken().getSubject();

        //check if user has ADMIN role
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(AuthoritiesConstants.ADMIN));
        //block if the user is not the author or admin
        if (!existingPost.getAuthorId().toString().equals(sub) && !isAdmin) {
            throw new AccessDeniedException(
                    "Errorr: Only the author or an Admin can delete this post.");
        }

        // 3. Delete associated media and reactions
        postMediaRepository.deleteByPostId(id);
        postReactionRepository.deleteByPostId(id);

        // 4. TODO: Namastack Outbox Event - Notify mediaService to clean up physical files via Kafka

        // 5. Delete the actual post
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
    public PostDTO createPost(String content,
            PostVisibility visibility,
            List<UUID> mediaIds,
            List<UUID> taggedUserIds) {
        LOG.debug("Request to create a new Post by current user");

        // 1. Extract user UUID from JWT Token 
        UUID authorId;
        Authentication auth = SecurityContextHolder
                .getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            String sub = jwtAuth.getToken().getSubject();
            authorId = UUID.fromString(sub);
        } else {
            throw new RuntimeException("Error: Cannot extract JWT token to get User ID.");
        }

        // 2. Initialize new Post entity
        Post newPost = new Post();
        newPost.setAuthorId(authorId);
        newPost.setContent(content);
        newPost.setVisibility(visibility);
        newPost.setStatus(PostStatus.ACTIVE);
        newPost.setCreatedAt(Instant.now());
        if (taggedUserIds != null && !taggedUserIds.isEmpty()) {
            newPost.setTaggedUserIds(new HashSet<>(taggedUserIds));
        }

        // 3. Save to Database
        newPost = postRepository.save(newPost);

        // 4. Save attached media files (if any)
        if (mediaIds != null && !mediaIds.isEmpty()) {
            int order = 0;
            for (UUID mediaId : mediaIds) {
                PostMedia postMedia = new PostMedia();
                postMedia.setPost(newPost);
                postMedia.setMediaId(mediaId);
                postMedia.setDisplayOrder(order++);
                postMedia.setCreatedAt(Instant.now());
                postMediaRepository.save(postMedia);
            }
        }

        return findOne(newPost.getId()).orElseThrow();
    }
    
    /**
     * Get one post by id, including its media attachments. Enforces privacy visibility rules.
     *
     * @param id the id of the entity.
     * @return the entity wrapped in Optional.
     * @throws AccessDeniedException if the current user lacks permission.
     */
    @Transactional(readOnly = true)
    public Optional<PostDTO> findOne(UUID id) {
        LOG.debug("Request to get Post : {}", id);
        return postRepository.findById(id).map(post -> {

                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                boolean isGuest = (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal()));
                boolean isAdmin = !isGuest && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(AuthoritiesConstants.ADMIN));


                if (post.getStatus() == PostStatus.DELETED && !isAdmin) {
                    throw new AccessDeniedException("Error: Post not found or has been deleted.");
                }
 

                // 1. Check Private Visibility
                if (post.getVisibility() == PostVisibility.PRIVATE) {
                    if (isGuest) {
                        throw new AccessDeniedException("Error: You do not have permission to view this private post.");
                    }
                    if (!isAdmin) {
                        String sub = ((JwtAuthenticationToken)
  auth).getToken().getSubject();
                        if (!post.getAuthorId().toString().equals(sub)) {
                            throw new AccessDeniedException("Error: Only the author can view this private post.");
                        }
                    }
                }

                // 2. Check Friends Visibility
                if (post.getVisibility() == PostVisibility.FRIENDS && !isAdmin) {
                    if (isGuest) {
                        throw new AccessDeniedException("Error: You must be logged in to view this friends-only post.");
                    }
                // TODO: Integrate with FriendshipService via FeignClient/Kafka
                }

                // 3. Convert to DTO
                PostDTO dto = postMapper.toDto(post);

                // 4. Fetch and attach media IDs
                List<UUID> mediaIds = postMediaRepository.findByPostIdOrderByDisplayOrderAsc(post.getId())
                        .stream()
                        .map(PostMedia::getMediaId)
                        .toList();
                dto.setMediaIds(mediaIds);

                return dto;
            });
    }
}

