package com.minh.fakebook.post.service;

import com.minh.fakebook.post.client.UserServiceClient;
import com.minh.fakebook.post.client.DownstreamServiceUnavailableException;
import com.minh.fakebook.post.domain.Post;
import com.minh.fakebook.post.domain.PostMedia;
import com.minh.fakebook.post.domain.enumeration.PostStatus;
import com.minh.fakebook.post.domain.enumeration.PostVisibility;
import com.minh.fakebook.post.repository.PostMediaRepository;
import com.minh.fakebook.post.repository.PostReactionRepository;
import com.minh.fakebook.post.repository.PostRepository;
import com.minh.fakebook.post.repository.SavedPostRepository;
import com.minh.fakebook.post.domain.SavedPost;
import com.minh.fakebook.post.security.AuthoritiesConstants;
import com.minh.fakebook.post.service.dto.PostDTO;
import com.minh.fakebook.post.service.event.PostCreatedEvent;
import com.minh.fakebook.post.service.event.PostDeletedEvent;
import com.minh.fakebook.post.service.event.PostUpdatedEvent;
import com.minh.fakebook.post.service.mapper.PostMapper;
import io.namastack.outbox.Outbox;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
import com.minh.fakebook.post.service.event.MediaCleanupEvent;
import com.minh.fakebook.post.client.MediaServiceClient;
import com.minh.fakebook.post.client.dto.MediaValidationDTO;
import com.minh.fakebook.post.service.event.EventEnvelope;

/**
 * Service Implementation for managing {@link Post}.
 */
@Service
@Transactional
public class PostService {

    private static final Logger LOG = LoggerFactory.getLogger(PostService.class);

    private final PostRepository postRepository;

    private final PostMapper postMapper;

    private final Outbox outbox;

    private final PostMediaRepository postMediaRepository;

    private final PostReactionRepository postReactionRepository;

    private final SavedPostRepository savedPostRepository;

    private final UserServiceClient userClient;

    private final MediaServiceClient mediaServiceClient;
    private final PostHistoryService history;

    public PostService(
            PostRepository postRepository,
            PostMapper postMapper,
            PostMediaRepository postMediaRepository,
            PostReactionRepository postReactionRepository,
            SavedPostRepository savedPostRepository,
            Outbox outbox,
            UserServiceClient userClient,
            MediaServiceClient mediaServiceClient, PostHistoryService history) {
        this.postRepository = postRepository;
        this.postMapper = postMapper;
        this.postMediaRepository = postMediaRepository;
        this.postReactionRepository = postReactionRepository;
        this.savedPostRepository = savedPostRepository;
        this.outbox = outbox;
        this.userClient = userClient;
        this.mediaServiceClient = mediaServiceClient;
        this.history = history;
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

        // Emit "POST_CREATED" event to Kafka via Outbox pattern for feedService fanout.
        publishEvent("POST_CREATED",
                new PostCreatedEvent(savedPost.getId(), savedPost.getAuthorId(), savedPost.getContent(),
                        savedPost.getVisibility(), savedPost.getStatus(),
                        savedPost.getCreatedAt()),
                savedPost.getId());
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
        return edit(postDTO, false);
    }

    public Optional<PostDTO> partialUpdate(PostDTO postDTO) {
        if (!postRepository.existsById(postDTO.getId())) return Optional.empty();
        return Optional.of(edit(postDTO, true));
    }

    private PostDTO edit(PostDTO request, boolean partial) {
        Post post = postRepository.findById(request.getId()).orElseThrow(() ->
            new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Post not found"));
        UUID editor = requireEditor(post);
        if (request.getVersion() != null && !request.getVersion().equals(post.getVersion())) {
            throw new org.springframework.dao.OptimisticLockingFailureException("Post changed; reload before editing");
        }
        PostDTO before = snapshot(post);
        List<UUID> media = request.getMediaIds() == null ? before.getMediaIds()
            : validateMediaAttachments(post.getAuthorId(), request.getMediaIds());
        java.util.Set<UUID> tags = request.getTaggedUserIds() == null ? new HashSet<>(post.getTaggedUserIds())
            : new HashSet<>(request.getTaggedUserIds());
        validateTags(post.getAuthorId(), tags, post.getTaggedUserIds());
        String content = partial && request.getContent() == null ? post.getContent() : request.getContent();
        PostVisibility visibility = request.getVisibility() == null ? post.getVisibility() : request.getVisibility();
        List<String> changed = new ArrayList<>();
        if (!java.util.Objects.equals(content, post.getContent())) changed.add("content");
        if (visibility != post.getVisibility()) changed.add("visibility");
        if (!media.equals(before.getMediaIds())) changed.add("media");
        if (!tags.equals(post.getTaggedUserIds())) changed.add("tags");
        if (changed.isEmpty()) return before;

        history.baseline(before);
        post.setContent(content);
        post.setVisibility(visibility);
        post.setTaggedUserIds(tags);
        post.setUpdatedAt(Instant.now());
        // Flush detects concurrent edits before either history or cleanup is committed.
        postRepository.saveAndFlush(post);
        if (changed.contains("media")) {
            postMediaRepository.deleteByPostId(post.getId());
            postMediaRepository.flush();
            List<PostMedia> links = new ArrayList<>();
            for (int i = 0; i < media.size(); i++) {
                links.add(new PostMedia().post(post).mediaId(media.get(i)).displayOrder(i).createdAt(Instant.now()));
            }
            postMediaRepository.saveAll(links);
            for (UUID old : before.getMediaIds()) if (!media.contains(old)) scheduleMediaCleanup(new MediaCleanupEvent(old, "POST UPDATED"));
        }
        PostDTO result = snapshot(post);
        history.record(result, editor, changed, post.getUpdatedAt());
        publishEvent("POST_UPDATED", new PostUpdatedEvent(post.getId(), post.getAuthorId(), post.getContent(),
            post.getVisibility(), post.getStatus(), post.getUpdatedAt(), post.getCreatedAt()), post.getId());
        return result;
    }

    private UUID requireEditor(Post post) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof JwtAuthenticationToken jwt)) throw new AccessDeniedException("JWT required");
        boolean admin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(AuthoritiesConstants.ADMIN));
        UUID user = UUID.fromString(jwt.getToken().getSubject());
        if (!admin && post.getStatus() != PostStatus.ACTIVE) throw new AccessDeniedException("Post is not active");
        if (!admin && !user.equals(post.getAuthorId())) throw new AccessDeniedException("Only the author or admin can edit this post");
        return user;
    }

    private void validateTags(UUID author, java.util.Set<UUID> tags, java.util.Set<UUID> previous) {
        if (tags.contains(null) || tags.contains(author)) throw new IllegalArgumentException("Invalid tagged user");
        var added = new HashSet<>(tags);
        added.removeAll(previous);
        if (!added.isEmpty() && !new HashSet<>(userClient.getFriendIdsByUserId(author)).containsAll(added)) {
            throw new AccessDeniedException("New tags must be friends of the author");
        }
    }

    private PostDTO snapshot(Post post) {
        PostDTO dto = postMapper.toDto(post);
        dto.setMediaIds(postMediaRepository.findByPostIdOrderByDisplayOrderAsc(post.getId()).stream().map(PostMedia::getMediaId).toList());
        return dto;
    }

    /**
     * Delete the post by id. Enforces authorship or ADMIN role, and cleans up local
     * links.
     *
     * @param id the id of the entity.
     * @throws AccessDeniedException if not the author or admin.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete Post : {}", id);
        // 1. Fetch existing post form DB
        Post existingPost = postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Error: Post not found " + id));

        // 2. Verify authorship or ADMIN role
        Authentication auth = SecurityContextHolder
                .getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new AccessDeniedException(
                    "Error: You must be logged in to delete a post.");
        }

        String sub = ((JwtAuthenticationToken) auth)
                .getToken().getSubject();

        // check if user has ADMIN role
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(AuthoritiesConstants.ADMIN));
        // block if the user is not the author or admin
        if (!existingPost.getAuthorId().toString().equals(sub) && !isAdmin) {
            throw new AccessDeniedException(
                    "Errorr: Only the author or an Admin can delete this post.");
        }

        // 3. Delete associated media and reactions
        java.util.List<UUID> mediaIds = postMediaRepository.findByPostIdOrderByDisplayOrderAsc(id)
                .stream()
                .map(com.minh.fakebook.post.domain.PostMedia::getMediaId)
                .toList();

        postMediaRepository.deleteByPostId(id);
        postReactionRepository.deleteByPostId(id);
        if (mediaIds != null) {
            for (UUID mediaId : mediaIds) {
                MediaCleanupEvent event = new MediaCleanupEvent(mediaId, "POST DELETED");
                scheduleMediaCleanup(event);
                LOG.info("Scheduled MediaCleanupEvent for deleted post mediaId: {}", mediaId);
            }
        }
        publishEvent("POST_DELETED", new PostDeletedEvent(id), id);
        // 5. Delete the actual post
        history.delete(id);
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

        validateTags(authorId, taggedUserIds == null ? java.util.Set.of() : new HashSet<>(taggedUserIds), java.util.Set.of());
        List<UUID> validatedMediaIds = validateMediaAttachments(authorId, mediaIds);

        // 2. Initialize new Post entity
        Post newPost = new Post();
        newPost.setAuthorId(authorId);
        newPost.setContent(content);
        newPost.setVisibility(visibility);
        newPost.setStatus(PostStatus.ACTIVE);
        newPost.setCreatedAt(java.time.Instant.now());
        if (taggedUserIds != null && !taggedUserIds.isEmpty()) {
            newPost.setTaggedUserIds(new HashSet<>(taggedUserIds));
        }

        // 3. Save to Database
        newPost = postRepository.saveAndFlush(newPost);

        // 4. Validate and Save attached media files (if any)
        if (!validatedMediaIds.isEmpty()) {
            List<PostMedia> postMediaList = new ArrayList<>();
            for (int i = 0; i < validatedMediaIds.size(); i++) {
                PostMedia postMedia = new PostMedia();
                postMedia.setPost(newPost);
                postMedia.setMediaId(validatedMediaIds.get(i));
                postMedia.setDisplayOrder(i);
                postMedia.setCreatedAt(java.time.Instant.now());
                postMediaList.add(postMedia);
            }
            postMediaRepository.saveAll(postMediaList);
        }
        PostDTO result = snapshot(newPost);
        history.baseline(result);
        publishEvent("POST_CREATED", new PostCreatedEvent(result.getId(), result.getAuthorId(), result.getContent(),
                result.getVisibility(), result.getStatus(), result.getCreatedAt()), result.getId());
        return findOne(newPost.getId()).orElseThrow();
    }

    /**
     * Get one post by id, including its media attachments. Enforces privacy
     * visibility rules.
     *
     * @param id the id of the entity.
     * @return the entity wrapped in Optional.
     * @throws AccessDeniedException if the current user lacks permission.
     */
    @Transactional(readOnly = true)
    public Optional<PostDTO> findOne(UUID id) {
        LOG.debug("Request to get Post : {}", id);
        return postRepository.findById(id).map(post -> {

            Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext()
                    .getAuthentication();
            boolean isGuest = (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal()));
            boolean isAdmin = !isGuest && auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals(com.minh.fakebook.post.security.AuthoritiesConstants.ADMIN));

            if (post.getStatus() == PostStatus.DELETED && !isAdmin) {
                throw new AccessDeniedException("Error: Post not found or has been deleted.");
            }

            // 1. Check Private Visibility
            if (post.getVisibility() == PostVisibility.PRIVATE) {
                if (isGuest) {
                    throw new AccessDeniedException("Error: You do not have permission to view this private post.");
                }
                if (!isAdmin) {
                    String sub = ((JwtAuthenticationToken) auth).getToken().getSubject();
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
                String sub = ((JwtAuthenticationToken) auth).getToken().getSubject();
                UUID currentUserId = UUID.fromString(sub);

                if (!post.getAuthorId().equals(currentUserId)) {
                    boolean areFriends = userClient.areFriends(currentUserId, post.getAuthorId());
                    if (!areFriends) {
                        throw new AccessDeniedException("Error: You must be a friend of the author to view this post.");
                    }
                }
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

    /**
     * Toggles the saved status of a post for the current user.
     *
     * @param postId the post ID to save/unsave.
     * @return true if the post is now saved, false if it is unsaved.
     */
    public boolean toggleSavePost(UUID postId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new AccessDeniedException("Error: You must be logged in to save a post.");
        }
        String sub = ((JwtAuthenticationToken) auth).getToken().getSubject();
        UUID userId = UUID.fromString(sub);

        // Verify post exists and is visible to the user
        findOne(postId).orElseThrow(() -> new IllegalArgumentException("Error: Post not found or not accessible"));

        Optional<SavedPost> existing = savedPostRepository.findByUserIdAndPostId(userId, postId);
        if (existing.isPresent()) {
            savedPostRepository.delete(existing.orElseThrow());
            return false; // Unsaved
        } else {
            SavedPost savedPost = new SavedPost();
            savedPost.setUserId(userId);
            savedPost.setPostId(postId);
            savedPost.setCreatedAt(Instant.now());
            savedPostRepository.save(savedPost);
            return true; // Saved
        }
    }

    /**
     * Gets all saved posts for the current user.
     *
     * @return list of saved posts.
     */
    @Transactional(readOnly = true)
    public List<PostDTO> getSavedPosts() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new AccessDeniedException("Error: You must be logged in to view saved posts.");
        }
        String sub = ((JwtAuthenticationToken) auth).getToken().getSubject();
        UUID userId = UUID.fromString(sub);

        List<SavedPost> savedPosts = savedPostRepository.findByUserIdOrderByCreatedAtDesc(userId);
        List<PostDTO> results = new ArrayList<>();

        for (SavedPost sp : savedPosts) {
            try {
                findOne(sp.getPostId()).ifPresent(results::add);
            } catch (Exception e) {
                // Ignore posts that are no longer accessible (e.g. deleted or privacy changed)
                LOG.warn("Skipping saved post {} for user {} due to access exception", sp.getPostId(), userId);
            }
        }
        return results;
    }

    private void publishEvent(String eventType, Object payload, UUID aggregateId) {
        EventEnvelope<Object> envelope = new EventEnvelope<>(
                UUID.randomUUID(), eventType, 1, java.time.Instant.now(), payload);
        outbox.schedule(envelope, "post-" + aggregateId);
    }

    private void scheduleMediaCleanup(MediaCleanupEvent event) {
        outbox.schedule(event, "media-cleanup-" + event.mediaId(),
                java.util.Map.of("destination", "media-cleanup-topic"));
    }

    private List<UUID> validateMediaAttachments(UUID authorId, List<UUID> mediaIds) {
        if (mediaIds == null || mediaIds.isEmpty()) {
            return List.of();
        }
        List<UUID> deduplicated = mediaIds.stream().distinct().toList();
        for (UUID mediaId : deduplicated) {
            if (mediaId == null) {
                throw new IllegalArgumentException("Error: mediaId cannot be null.");
            }
            try {
                MediaValidationDTO mediaInfo = mediaServiceClient.getMedia(mediaId);
                if (mediaInfo == null || !authorId.equals(mediaInfo.ownerId())
                        || !"ACTIVE".equals(mediaInfo.status())
                        || !"POST".equals(mediaInfo.purpose())) {
                    throw new AccessDeniedException("Invalid media ownership, status or purpose: " + mediaId);
                }
            } catch (DownstreamServiceUnavailableException exception) {
                throw exception;
            } catch (AccessDeniedException | org.springframework.web.server.ResponseStatusException exception) {
                throw exception;
            } catch (IllegalArgumentException exception) {
                throw exception;
            } catch (Exception exception) {
                throw new IllegalArgumentException("Error: Media validation failed for ID " + mediaId, exception);
            }
        }
        return deduplicated;
    }
}
