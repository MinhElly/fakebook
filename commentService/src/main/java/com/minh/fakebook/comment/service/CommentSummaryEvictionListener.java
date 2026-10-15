package com.minh.fakebook.comment.service;

import com.minh.fakebook.comment.service.event.CommentSummaryChanged;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listens for comment summary changes and evicts the corresponding cache entry after transaction commit.
 */
@Component
public class CommentSummaryEvictionListener {

    private static final Logger LOG = LoggerFactory.getLogger(CommentSummaryEvictionListener.class);

    private final CommentSummaryCache summaryCache;

    public CommentSummaryEvictionListener(CommentSummaryCache summaryCache) {
        this.summaryCache = summaryCache;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onCommentSummaryChanged(CommentSummaryChanged event) {
        if (event == null || event.postId() == null) {
            return;
        }
        try {
            summaryCache.evict(event.postId());
        } catch (Exception ex) {
            LOG.warn("Failed to evict comment summary cache for post {}", event.postId(), ex);
        }
    }
}
