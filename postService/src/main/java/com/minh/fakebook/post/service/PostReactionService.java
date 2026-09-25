package com.minh.fakebook.post.service;

import com.minh.fakebook.post.client.UserServiceClient;
import com.minh.fakebook.post.domain.Post;
import com.minh.fakebook.post.domain.PostReaction;
import com.minh.fakebook.post.domain.enumeration.PostStatus;
import com.minh.fakebook.post.domain.enumeration.PostVisibility;
import com.minh.fakebook.post.domain.enumeration.ReactionType;
import com.minh.fakebook.post.repository.PostReactionRepository;
import com.minh.fakebook.post.repository.PostRepository;
import com.minh.fakebook.post.security.AuthoritiesConstants;
import com.minh.fakebook.post.service.dto.PostReactionDTO;
import com.minh.fakebook.post.service.dto.PostReactionSummaryDTO;
import com.minh.fakebook.post.service.dto.PostReactorDTO;
import com.minh.fakebook.post.service.event.EventEnvelope;
import com.minh.fakebook.post.service.event.PostReactionChangedEvent;
import com.minh.fakebook.post.service.mapper.PostReactionMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import io.namastack.outbox.Outbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class PostReactionService {

    private static final Logger LOG = LoggerFactory.getLogger(PostReactionService.class);
    private static final int MAX_BATCH_SIZE = 50;

    private final PostReactionRepository postReactionRepository;
    private final PostReactionMapper postReactionMapper;
    private final PostRepository postRepository;
    private final UserServiceClient userServiceClient;
    private final Outbox outbox;

    public PostReactionService(
        PostReactionRepository postReactionRepository,
        PostReactionMapper postReactionMapper,
        PostRepository postRepository,
        UserServiceClient userServiceClient,
        Outbox outbox
    ) {
        this.postReactionRepository = postReactionRepository;
        this.postReactionMapper = postReactionMapper;
        this.postRepository = postRepository;
        this.userServiceClient = userServiceClient;
        this.outbox = outbox;
    }

    /**
     * Legacy generated endpoint. New clients should use setReaction/removeCurrentUserReaction.
     */
    public PostReactionDTO save(PostReactionDTO postReactionDTO) {
        LOG.debug("Request to upsert/toggle PostReaction: {}", postReactionDTO);

        UUID currentUserId = requireCurrentUserId();
        postReactionDTO.setUserId(currentUserId);

        if (postReactionDTO.getPost() == null || postReactionDTO.getPost().getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "post.id is required");
        }

        UUID postId = postReactionDTO.getPost().getId();
        Post targetPost = findPost(postId);
        checkCanInteract(targetPost, currentUserId);

        Optional<PostReaction> existingReaction = postReactionRepository.findByPostIdAndUserId(postId, currentUserId);

        if (existingReaction.isPresent()) {
            PostReaction reaction = existingReaction.orElseThrow();
            if (reaction.getReactionType() == postReactionDTO.getReactionType()) {
                postReactionRepository.delete(reaction);
                return null;
            }

            reaction.setReactionType(postReactionDTO.getReactionType());
            reaction.setUpdatedAt(Instant.now());
            return postReactionMapper.toDto(postReactionRepository.save(reaction));
        }

        PostReaction reaction = postReactionMapper.toEntity(postReactionDTO);
        reaction.setUserId(currentUserId);
        reaction.setPost(targetPost);
        reaction.setCreatedAt(Instant.now());
        return postReactionMapper.toDto(postReactionRepository.save(reaction));
    }

    public PostReactionDTO update(PostReactionDTO postReactionDTO) {
        throw new UnsupportedOperationException("Direct update is disabled. Use the reaction endpoint.");
    }

    public Optional<PostReactionDTO> partialUpdate(PostReactionDTO postReactionDTO) {
        throw new UnsupportedOperationException("Partial update is disabled.");
    }

    @Transactional(readOnly = true)
    public Optional<PostReactionDTO> findOne(UUID id) {
        LOG.debug("Request to get PostReaction: {}", id);
        return postReactionRepository.findById(id).map(postReactionMapper::toDto);
    }

    public void delete(UUID id) {
        PostReaction reaction = postReactionRepository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reaction not found"));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = authentication != null &&
            authentication.getAuthorities().stream().anyMatch(authority -> authority.getAuthority().equals(AuthoritiesConstants.ADMIN));
        UUID currentUserId = requireCurrentUserId();

        if (!isAdmin && !reaction.getUserId().equals(currentUserId)) {
            throw new AccessDeniedException("You can only delete your own reaction");
        }

        postReactionRepository.delete(reaction);
    }

    public PostReactionSummaryDTO setReaction(UUID postId, ReactionType requestedType) {
        UUID currentUserId = requireCurrentUserId();
        Post post = findPost(postId);
        checkCanInteract(post, currentUserId);

        Optional<PostReaction> existing = postReactionRepository.findByPostIdAndUserId(postId, currentUserId);

        boolean changed = false;

        if (existing.isPresent()) {
            PostReaction reaction = existing.orElseThrow();
            if (reaction.getReactionType() != requestedType) {
                reaction.setReactionType(requestedType);
                reaction.setUpdatedAt(Instant.now());
                postReactionRepository.saveAndFlush(reaction);
                changed = true;
            }
        } else {
            PostReaction reaction = new PostReaction();
            reaction.setUserId(currentUserId);
            reaction.setReactionType(requestedType);
            reaction.setCreatedAt(Instant.now());
            reaction.setPost(post);
            postReactionRepository.saveAndFlush(reaction);
            changed = true;
        }
        if (changed) {
            publishReactionChanged(postId, "SET");
        }

        return getSummary(postId, currentUserId);
    }

    public PostReactionSummaryDTO removeCurrentUserReaction(UUID postId) {
        UUID currentUserId = requireCurrentUserId();
        Post post = findPost(postId);
        checkCanInteract(post, currentUserId);
        Optional<PostReaction> existing = postReactionRepository.findByPostIdAndUserId(postId, currentUserId);

        if (existing.isPresent()) {
            postReactionRepository.delete(existing.orElseThrow());
            postReactionRepository.flush();
            publishReactionChanged(postId, "REMOVE");
        }

        return getSummary(postId, currentUserId);
    }

    @Transactional(readOnly = true)
    public List<PostReactionSummaryDTO> getSummaries(Collection<UUID> requestedPostIds) {
        UUID currentUserId = requireCurrentUserId();
        Set<UUID> distinctPostIds = new LinkedHashSet<>(requestedPostIds);

        if (distinctPostIds.isEmpty()) {
            return List.of();
        }
        if (distinctPostIds.size() > MAX_BATCH_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Maximum 50 post IDs per request");
        }

        List<Post> posts = postRepository.findAllById(distinctPostIds);
        boolean needsFriendIds = posts
            .stream()
            .anyMatch(post -> post.getVisibility() == PostVisibility.FRIENDS && !post.getAuthorId().equals(currentUserId));
        Set<UUID> friendIds = needsFriendIds
            ? new HashSet<>(userServiceClient.getFriendIdsByUserId(currentUserId))
            : Set.of();
        List<UUID> visiblePostIds = posts
            .stream()
            .filter(post -> canView(post, currentUserId, friendIds))
            .map(Post::getId)
            .toList();

        return buildSummaries(visiblePostIds, currentUserId);
    }

    @Transactional(readOnly = true)
    public Page<PostReactorDTO> getReactors(UUID postId, Pageable pageable) {
        UUID currentUserId = requireCurrentUserId();
        Post post = findPost(postId);
        checkCanInteract(post, currentUserId);

        return postReactionRepository.findAllByPostId(postId, pageable).map(reaction -> {
            Instant reactedAt = reaction.getUpdatedAt() != null ? reaction.getUpdatedAt() : reaction.getCreatedAt();
            return new PostReactorDTO(reaction.getUserId(), reaction.getReactionType(), reactedAt);
        });
    }

    private List<PostReactionSummaryDTO> buildSummaries(List<UUID> postIds, UUID currentUserId) {
        if (postIds.isEmpty()) {
            return List.of();
        }

        Map<UUID, EnumMap<ReactionType, Long>> countsByPost = new HashMap<>();
        for (UUID postId : postIds) {
            countsByPost.put(postId, emptyCounts());
        }

        for (PostReactionRepository.ReactionCountProjection row : postReactionRepository.countByPostIds(postIds)) {
            countsByPost.get(row.getPostId()).put(row.getReactionType(), row.getReactionCount());
        }

        Map<UUID, ReactionType> myReactionByPost = new HashMap<>();
        for (PostReaction reaction : postReactionRepository.findAllByPostIdInAndUserId(postIds, currentUserId)) {
            myReactionByPost.put(reaction.getPost().getId(), reaction.getReactionType());
        }

        List<PostReactionSummaryDTO> result = new ArrayList<>();
        for (UUID postId : postIds) {
            EnumMap<ReactionType, Long> counts = countsByPost.get(postId);
            long total = counts.values().stream().mapToLong(Long::longValue).sum();
            result.add(new PostReactionSummaryDTO(postId, total, counts, myReactionByPost.get(postId)));
        }
        return result;
    }

    private PostReactionSummaryDTO getSummary(UUID postId, UUID currentUserId) {
        return buildSummaries(List.of(postId), currentUserId).getFirst();
    }

    private EnumMap<ReactionType, Long> emptyCounts() {
        EnumMap<ReactionType, Long> counts = new EnumMap<>(ReactionType.class);
        for (ReactionType type : ReactionType.values()) {
            counts.put(type, 0L);
        }
        return counts;
    }

    private UUID requireCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwt)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        try {
            return UUID.fromString(jwt.getToken().getSubject());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "JWT subject is not a UUID");
        }
    }

    private Post findPost(UUID postId) {
        return postRepository
            .findById(postId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));
    }

    private void checkCanInteract(Post post, UUID currentUserId) {
        if (post.getStatus() == PostStatus.DELETED) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found");
        }
        if (post.getAuthorId().equals(currentUserId) || post.getVisibility() == PostVisibility.PUBLIC) {
            return;
        }
        if (post.getVisibility() == PostVisibility.PRIVATE) {
            throw new AccessDeniedException("Cannot access this private post");
        }
        if (post.getVisibility() == PostVisibility.FRIENDS && userServiceClient.areFriends(currentUserId, post.getAuthorId())) {
            return;
        }
        throw new AccessDeniedException("Cannot access this post");
    }

    private boolean canView(Post post, UUID currentUserId, Set<UUID> friendIds) {
        if (post.getStatus() == PostStatus.DELETED) {
            return false;
        }
        if (post.getAuthorId().equals(currentUserId) || post.getVisibility() == PostVisibility.PUBLIC) {
            return true;
        }
        return post.getVisibility() == PostVisibility.FRIENDS && friendIds.contains(post.getAuthorId());
    }
    private void publishReactionChanged(UUID postId, String action) {
        Instant occurredAt = Instant.now();
        EventEnvelope<PostReactionChangedEvent> envelope = new EventEnvelope<>(
            UUID.randomUUID(),
            "POST_REACTION_CHANGED",
            1,
            occurredAt,
            new PostReactionChangedEvent(postId, action, occurredAt)
        );
        outbox.schedule(envelope, "post-reaction-" + postId, Map.of("destination", "post-reaction-events"));
    }
}
