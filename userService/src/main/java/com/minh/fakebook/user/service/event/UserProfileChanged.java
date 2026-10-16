package com.minh.fakebook.user.service.event;

import java.util.UUID;

/**
 * Event published when a user profile is saved, updated, or deleted.
 * Used for post-commit cache eviction.
 */
public record UserProfileChanged(UUID userId) {}
