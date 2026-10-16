package com.minh.fakebook.post.service;

import com.minh.fakebook.post.service.event.ReactionCountsChanged;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Event listener that invalidates reaction counts cache only after the DB transaction has committed.
 */
@Component
public class PostReactionCountsEvictionListener {

    private static final Logger LOG = LoggerFactory.getLogger(PostReactionCountsEvictionListener.class);

    private final PostReactionCountsCache countsCache;

    public PostReactionCountsEvictionListener(PostReactionCountsCache countsCache) {
        this.countsCache = countsCache;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onReactionCountsChanged(ReactionCountsChanged event) {
        LOG.debug("Received ReactionCountsChanged event for post: {}, evicting cache", event.postId());
        countsCache.evict(event.postId());
    }
}
