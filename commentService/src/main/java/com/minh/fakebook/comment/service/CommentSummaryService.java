package com.minh.fakebook.comment.service;

import com.minh.fakebook.comment.domain.Comment;
import com.minh.fakebook.comment.domain.enumeration.CommentStatus;
import com.minh.fakebook.comment.repository.CommentRepository;
import com.minh.fakebook.comment.service.dto.CommentPreviewDTO;
import com.minh.fakebook.comment.service.dto.CommentSummaryDTO;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CommentSummaryService {

    private final CommentRepository commentRepository;

    private final CommentViewAuthorizationService authorizationService;

    public CommentSummaryService(CommentRepository commentRepository, CommentViewAuthorizationService authorizationService) {
        this.commentRepository = commentRepository;
        this.authorizationService = authorizationService;
    }

    public List<CommentSummaryDTO> getSummaries(List<UUID> requestedPostIds) {
        LinkedHashSet<UUID> postIds = new LinkedHashSet<>(requestedPostIds);
        authorizationService.verifyCanView(postIds);

        Map<UUID, Long> counts = commentRepository
            .countByPostIdsAndStatus(postIds, CommentStatus.ACTIVE)
            .stream()
            .collect(
                Collectors.toMap(
                    CommentRepository.CommentCountProjection::getPostId,
                    CommentRepository.CommentCountProjection::getCommentCount
                )
            );
        Map<UUID, Comment> previews = commentRepository
            .findLatestActiveRootComments(postIds)
            .stream()
            .collect(Collectors.toMap(Comment::getPostId, Function.identity()));

        return postIds
            .stream()
            .map(postId -> new CommentSummaryDTO(postId, counts.getOrDefault(postId, 0L), toPreview(previews.get(postId))))
            .toList();
    }

    private CommentPreviewDTO toPreview(Comment comment) {
        if (comment == null) {
            return null;
        }
        return new CommentPreviewDTO(comment.getId(), comment.getAuthorId(), comment.getContent(), comment.getCreatedAt());
    }
}
