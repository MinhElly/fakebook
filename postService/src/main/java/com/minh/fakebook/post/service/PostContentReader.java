package com.minh.fakebook.post.service;

import com.minh.fakebook.post.domain.Post;
import com.minh.fakebook.post.domain.PostMedia;
import com.minh.fakebook.post.repository.PostMediaRepository;
import com.minh.fakebook.post.repository.PostRepository;
import com.minh.fakebook.post.service.dto.PostContentCacheDTO;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reader component responsible for loading posts and media attachments from MariaDB.
 * Eliminates N+1 query problem by batch loading media in a single query.
 */
@Component
public class PostContentReader {

    private final PostRepository postRepository;
    private final PostMediaRepository postMediaRepository;

    public PostContentReader(PostRepository postRepository, PostMediaRepository postMediaRepository) {
        this.postRepository = postRepository;
        this.postMediaRepository = postMediaRepository;
    }

    /**
     * Loads post content for a batch of post IDs directly from the database.
     * Missing IDs are populated with {@link PostContentCacheDTO#EMPTY} for negative caching.
     *
     * @param postIds collection of post IDs
     * @return map of postId to PostContentCacheDTO
     */
    @Transactional(readOnly = true)
    public Map<UUID, PostContentCacheDTO> loadFromDatabase(Collection<UUID> postIds) {
        if (postIds == null || postIds.isEmpty()) {
            return Map.of();
        }

        List<Post> posts = postRepository.findAllById(postIds);
        List<PostMedia> mediaList = postMediaRepository.findByPostIdInOrderByDisplayOrderAsc(postIds);

        Map<UUID, List<UUID>> mediaByPostId = new HashMap<>();
        for (PostMedia pm : mediaList) {
            if (pm.getPost() != null && pm.getPost().getId() != null && pm.getMediaId() != null) {
                mediaByPostId.computeIfAbsent(pm.getPost().getId(), k -> new ArrayList<>()).add(pm.getMediaId());
            }
        }

        Map<UUID, PostContentCacheDTO> result = new HashMap<>();
        for (Post post : posts) {
            List<UUID> mediaIds = mediaByPostId.getOrDefault(post.getId(), List.of());
            result.put(
                post.getId(),
                new PostContentCacheDTO(
                    post.getId(),
                    post.getAuthorId(),
                    post.getContent(),
                    post.getVisibility(),
                    post.getStatus(),
                    post.getCreatedAt(),
                    post.getUpdatedAt(),
                    mediaIds,
                    post.getTaggedUserIds(),
                    post.getVersion()
                )
            );
        }

        // Negative caching: mark missing post IDs as EMPTY
        for (UUID requestedId : postIds) {
            result.putIfAbsent(requestedId, PostContentCacheDTO.EMPTY);
        }

        return result;
    }
}
