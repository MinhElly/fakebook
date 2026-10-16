package com.minh.fakebook.user.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.minh.fakebook.user.IntegrationTest;
import com.minh.fakebook.user.client.MediaServiceClient;
import com.minh.fakebook.user.domain.UserProfile;
import com.minh.fakebook.user.repository.UserProfileRepository;
import com.minh.fakebook.user.service.dto.UserProfileDTO;
import com.minh.fakebook.user.service.mapper.UserProfileMapper;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@AutoConfigureMockMvc
@Transactional
class UserPresenceServiceIT {

    private static final Instant EARLIER =
        Instant.parse("2026-10-16T03:00:00Z");

    private static final Instant LATER =
        Instant.parse("2026-10-16T03:05:00Z");

    @Autowired
    private UserPresenceService userPresenceService;

    @Autowired
    private UserProfileService userProfileService;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private UserProfileMapper userProfileMapper;

    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private MediaServiceClient mediaServiceClient;

    private UUID userId;

    @BeforeEach
    void createProfile() {
        userId = UUID.randomUUID();

        UserProfile profile = new UserProfile()
            .id(userId)
            .username("p-" + userId)
            .displayName("Presence test")
            .createdAt(EARLIER);

        userProfileRepository.saveAndFlush(profile);
        entityManager.clear();
    }

    @Test
    void shouldStartWithoutLastSeenAndAcceptFirstTimestamp() {
        assertThat(reloadProfile().getLastSeenAt()).isNull();

        boolean updated = userPresenceService.recordLastSeen(
            userId,
            EARLIER
        );

        assertThat(updated).isTrue();
        assertThat(reloadProfile().getLastSeenAt())
            .isEqualTo(EARLIER);
    }

    @Test
    void shouldAcceptNewerAndIgnoreOlderOrEqualTimestamp() {
        assertThat(
            userPresenceService.recordLastSeen(userId, EARLIER)
        ).isTrue();

        assertThat(
            userPresenceService.recordLastSeen(userId, LATER)
        ).isTrue();

        assertThat(
            userPresenceService.recordLastSeen(userId, EARLIER)
        ).isFalse();

        assertThat(
            userPresenceService.recordLastSeen(userId, LATER)
        ).isFalse();

        assertThat(reloadProfile().getLastSeenAt())
            .isEqualTo(LATER);
    }

    @Test
    void shouldNotCreateMissingProfile() {
        UUID missingId = UUID.randomUUID();

        boolean updated = userPresenceService.recordLastSeen(
            missingId,
            LATER
        );

        assertThat(updated).isFalse();
        assertThat(userProfileRepository.existsById(missingId))
            .isFalse();
    }

    @Test
    void fullProfileUpdateShouldPreserveLastSeen() {
        userPresenceService.recordLastSeen(userId, LATER);

        UserProfileDTO dto = userProfileMapper.toDto(
            reloadProfile()
        );
        dto.setDisplayName("Updated by PUT");

        userProfileService.update(dto);

        UserProfile saved = reloadProfile();

        assertThat(saved.getDisplayName())
            .isEqualTo("Updated by PUT");
        assertThat(saved.getLastSeenAt())
            .isEqualTo(LATER);
    }

    @Test
    void partialProfileUpdateShouldPreserveLastSeen() {
        userPresenceService.recordLastSeen(userId, LATER);

        UserProfileDTO dto = new UserProfileDTO();
        dto.setId(userId);
        dto.setDisplayName("Updated by PATCH");

        assertThat(
            userProfileService.partialUpdate(dto)
        ).isPresent();

        UserProfile saved = reloadProfile();

        assertThat(saved.getDisplayName())
            .isEqualTo("Updated by PATCH");
        assertThat(saved.getLastSeenAt())
            .isEqualTo(LATER);
    }

    @Test
    void staleProfileShouldNotOverwriteNewerDatabaseTimestamp() {
        userPresenceService.recordLastSeen(userId, EARLIER);

        UserProfile staleProfile = reloadProfile();
        UserProfileDTO dto = userProfileMapper.toDto(staleProfile);

        // Update database directly, keeping the managed entity stale.
        int changed = entityManager.createQuery("""
            update UserProfile u
               set u.lastSeenAt = :lastSeenAt
             where u.id = :userId
            """)
            .setParameter("lastSeenAt", LATER)
            .setParameter("userId", userId)
            .executeUpdate();

        assertThat(changed).isEqualTo(1);
        assertThat(staleProfile.getLastSeenAt())
            .isEqualTo(EARLIER);

        dto.setDisplayName("Updated from stale profile");
        userProfileService.update(dto);

        UserProfile saved = reloadProfile();

        assertThat(saved.getDisplayName())
            .isEqualTo("Updated from stale profile");
        assertThat(saved.getLastSeenAt())
            .isEqualTo(LATER);
    }

    private UserProfile reloadProfile() {
        entityManager.flush();
        entityManager.clear();

        return userProfileRepository.findById(userId)
            .orElseThrow();
    }
}
