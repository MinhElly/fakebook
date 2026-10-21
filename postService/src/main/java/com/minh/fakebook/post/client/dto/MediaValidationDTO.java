package com.minh.fakebook.post.client.dto;

import java.util.UUID;

/**
 * DTO for validating media ownership and status.
 */
public record MediaValidationDTO(UUID id, UUID ownerId, String status, String purpose, UUID publicationId) {
    public MediaValidationDTO(UUID id, UUID ownerId, String status, String purpose) { this(id, ownerId, status, purpose, null); }
}