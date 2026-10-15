package com.minh.fakebook.user.service;

import com.minh.fakebook.user.service.event.UserProfileChanged;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Transactional event listener that evicts user profile caches after successful commit.
 */
@Component
public class UserProfileEvictionListener {

    private static final Logger LOG = LoggerFactory.getLogger(UserProfileEvictionListener.class);

    private final UserProfileCache userProfileCache;

    public UserProfileEvictionListener(UserProfileCache userProfileCache) {
        this.userProfileCache = userProfileCache;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onUserProfileChanged(UserProfileChanged event) {
        LOG.debug("Evicting user profile caches for user: {}", event.userId());
        userProfileCache.evict(event.userId());
    }
}
