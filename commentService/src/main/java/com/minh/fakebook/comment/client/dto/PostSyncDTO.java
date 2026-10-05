package com.minh.fakebook.comment.client.dto;
import java.util.UUID;
public record PostSyncDTO(UUID id, UUID authorId, String status, String visibility) {}

