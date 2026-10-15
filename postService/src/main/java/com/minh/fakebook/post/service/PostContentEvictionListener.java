package com.minh.fakebook.post.service;

import com.minh.fakebook.post.service.event.PostContentChanged;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Event listener that invalidates post content cache only after the DB transaction has committed.
 */
@Component
public class PostContentEvictionListener {

    private static final Logger LOG = LoggerFactory.getLogger(PostContentEvictionListener.class);

    private final PostContentCache postContentCache;

    public PostContentEvictionListener(PostContentCache postContentCache) {
        this.postContentCache = postContentCache;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onPostContentChanged(PostContentChanged event) {
        LOG.debug("Received PostContentChanged event for post: {}, evicting cache", event.postId());
        postContentCache.evict(event.postId());
    }
}
