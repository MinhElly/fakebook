package com.minh.fakebook.post.service.event;

import java.util.UUID;

/**
 * Internal Spring application event indicating that post content or metadata has changed in MariaDB.
 * Triggers cache eviction after transaction commit.
 */
public record PostContentChanged(UUID postId) {
}
