package com.minh.fakebook.user.service;

import com.minh.fakebook.user.repository.UserProfileRepository;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserPresenceService {

    private final UserProfileRepository userProfileRepository;

    public UserPresenceService(
        UserProfileRepository userProfileRepository
    ) {
        this.userProfileRepository = userProfileRepository;
    }

    public record ReadablePresence(UUID userId, Instant lastSeenAt) {}

    @Transactional(readOnly = true)
    public java.util.List<ReadablePresence> readablePresence(UUID viewerId, java.util.Set<UUID> targetIds) {
        if (!userProfileRepository.existsById(viewerId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Viewer profile not found");
        }
        if (targetIds.isEmpty()) return java.util.List.of();
        return userProfileRepository.findReadablePresence(viewerId, targetIds).stream()
            .map(row -> new ReadablePresence(row.getId(), row.getLastSeenAt())).toList();
    }

    @Transactional
    public boolean recordLastSeen(
        UUID userId,
        Instant lastSeenAt
    ) {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(
            lastSeenAt,
            "lastSeenAt must not be null"
        );

        return userProfileRepository.updateLastSeenAtIfNewer(
            userId,
            lastSeenAt
        ) > 0;
    }
    @Transactional
    public void recordLastSeenForExistingUser(
        UUID userId,
        Instant lastSeenAt
    ) {
        boolean updated = recordLastSeen(userId, lastSeenAt);
        if(!updated && !userProfileRepository.existsById(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User profile not found");
        }
    }
}
