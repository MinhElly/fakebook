package com.minh.fakebook.post.service;

import com.minh.fakebook.post.domain.enumeration.ReactionType;
import com.minh.fakebook.post.repository.PostReactionRepository;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reader component responsible for loading aggregated reaction counts from MariaDB.
 */
@Component
public class PostReactionCountsReader {

    private final PostReactionRepository postReactionRepository;

    public PostReactionCountsReader(PostReactionRepository postReactionRepository) {
        this.postReactionRepository = postReactionRepository;
    }

    /**
     * Loads reaction counts for a batch of post IDs directly from the database.
     *
     * @param postIds collection of post IDs
     * @return map of postId to reaction counts map, fully initialized with 0 for all reaction types
     */
    @Transactional(readOnly = true)
    public Map<UUID, Map<ReactionType, Long>> loadFromDatabase(Collection<UUID> postIds) {
        if (postIds == null || postIds.isEmpty()) {
            return Map.of();
        }

        Map<UUID, Map<ReactionType, Long>> result = new HashMap<>();
        for (UUID postId : postIds) {
            result.put(postId, emptyCounts());
        }

        for (PostReactionRepository.ReactionCountProjection row : postReactionRepository.countByPostIds(postIds)) {
            Map<ReactionType, Long> counts = result.get(row.getPostId());
            if (counts != null) {
                counts.put(row.getReactionType(), row.getReactionCount());
            }
        }

        return result;
    }

    public Map<ReactionType, Long> emptyCounts() {
        EnumMap<ReactionType, Long> counts = new EnumMap<>(ReactionType.class);
        for (ReactionType type : ReactionType.values()) {
            counts.put(type, 0L);
        }
        return counts;
    }
}
