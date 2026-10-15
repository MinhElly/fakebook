package com.minh.fakebook.comment.service;

import com.minh.fakebook.comment.domain.Comment;
import com.minh.fakebook.comment.domain.enumeration.CommentStatus;
import com.minh.fakebook.comment.repository.CommentRepository;
import com.minh.fakebook.comment.service.dto.CommentPreviewDTO;
import com.minh.fakebook.comment.service.dto.CommentSummaryCacheDTO;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads comment summary data (counts and preview comments) from the database in batch.
 */
@Component
public class CommentSummaryReader {

    private final CommentRepository commentRepository;

    public CommentSummaryReader(CommentRepository commentRepository) {
        this.commentRepository = commentRepository;
    }

    /**
     * Loads comment summaries for given post IDs directly from the database.
     *
     * @param postIds collection of post IDs
     * @return map of post ID to CommentSummaryCacheDTO
     */
    @Transactional(readOnly = true)
    public Map<UUID, CommentSummaryCacheDTO> loadFromDatabase(Collection<UUID> postIds) {
        if (postIds == null || postIds.isEmpty()) {
            return Map.of();
        }

        Map<UUID, Long> counts = commentRepository
            .countByPostIdsAndStatus(postIds, CommentStatus.ACTIVE)
            .stream()
            .collect(
                Collectors.toMap(
                    CommentRepository.CommentCountProjection::getPostId,
                    CommentRepository.CommentCountProjection::getCommentCount,
                    (existing, replacement) -> existing
                )
            );

        Map<UUID, Comment> previews = commentRepository
            .findLatestActiveRootComments(postIds)
            .stream()
            .collect(Collectors.toMap(Comment::getPostId, Function.identity(), (existing, replacement) -> existing));

        Map<UUID, CommentSummaryCacheDTO> result = new HashMap<>();
        for (UUID postId : postIds) {
            long count = counts.getOrDefault(postId, 0L);
            CommentPreviewDTO preview = toPreview(previews.get(postId));
            result.put(postId, new CommentSummaryCacheDTO(count, preview));
        }

        return result;
    }

    private CommentPreviewDTO toPreview(Comment comment) {
        if (comment == null) {
            return null;
        }
        return new CommentPreviewDTO(comment.getId(), comment.getAuthorId(), comment.getContent(), comment.getCreatedAt());
    }
}
