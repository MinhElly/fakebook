package com.minh.fakebook.user.web.rest;

import com.minh.fakebook.user.security.AuthoritiesConstants;
import com.minh.fakebook.user.service.UserPresenceService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;
@RestController
@RequestMapping("/api/internal/presence")
@PreAuthorize(
    "hasAuthority('" + AuthoritiesConstants.INTERNAL + "')"
)
public class InternalPresenceResource {
    private final UserPresenceService userPresenceService;

    public InternalPresenceResource(UserPresenceService userPresenceService) {
        this.userPresenceService = userPresenceService;
    }

    public record LastSeenRequest(@NotNull Instant lastSeenAt) {}

    public record ReadRequest(
        @NotNull UUID viewerId,
        @NotNull @jakarta.validation.constraints.Size(max = 100) java.util.Set<@NotNull UUID> userIds
    ) {}

    @PostMapping("/readable")
    public java.util.List<UserPresenceService.ReadablePresence> readable(@Valid @RequestBody ReadRequest request) {
        return userPresenceService.readablePresence(request.viewerId(), request.userIds());
    }

    @PutMapping("/{userId}/last-seen")
    public ResponseEntity<Void> updateLastSeen(
        @PathVariable("userId") UUID userId,
        @Valid @RequestBody LastSeenRequest request
    ) {
        userPresenceService.recordLastSeenForExistingUser(
            userId,
            request.lastSeenAt()
        );

        return ResponseEntity.noContent().build();
    }
}
