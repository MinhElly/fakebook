package com.minh.fakebook.comment.service;

import com.minh.fakebook.comment.service.dto.CommentPreviewDTO;
import com.minh.fakebook.comment.service.dto.CommentSummaryCacheDTO;
import com.minh.fakebook.comment.service.dto.CommentSummaryDTO;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CommentSummaryService {

    private final CommentViewAuthorizationService authorizationService;
    private final CommentSummaryReader summaryCache;

    public CommentSummaryService(
        CommentViewAuthorizationService authorizationService,
        CommentSummaryReader summaryCache
    ) {
        this.authorizationService = authorizationService;
        this.summaryCache = summaryCache;
    }

    public List<CommentSummaryDTO> getSummaries(List<UUID> requestedPostIds) {
        LinkedHashSet<UUID> postIds = new LinkedHashSet<>(requestedPostIds);
        authorizationService.verifyCanView(postIds);

        Map<UUID, CommentSummaryCacheDTO> summaries = summaryCache.loadFromDatabase(postIds);

        return postIds
            .stream()
            .map(postId -> {
                CommentSummaryCacheDTO dto = summaries.get(postId);
                long count = dto != null ? dto.commentCount() : 0L;
                CommentPreviewDTO preview = dto != null ? dto.previewComment() : null;
                return new CommentSummaryDTO(postId, count, preview);
            })
            .toList();
    }
}
