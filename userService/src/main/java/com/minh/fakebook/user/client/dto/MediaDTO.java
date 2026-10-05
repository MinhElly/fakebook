package com.minh.fakebook.user.client.dto;
import java.util.UUID;
public record MediaDTO(UUID id, UUID ownerId, String url, String status) {}

