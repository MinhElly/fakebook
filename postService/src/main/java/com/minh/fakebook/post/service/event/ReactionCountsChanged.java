package com.minh.fakebook.post.service.event;

import java.util.UUID;

/**
 * Internal Spring application event indicating that reaction counts for a post have changed in MariaDB.
 * Triggers cache eviction after transaction commit.
 */
public record ReactionCountsChanged(UUID postId) {
}
