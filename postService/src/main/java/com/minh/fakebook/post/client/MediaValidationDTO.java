package com.minh.fakebook.post.client;

import java.util.UUID;

/**
 * DTO for validating media ownership and status.
 */
public record MediaValidationDTO(UUID id, UUID ownerId, String status, String purpose) {
}