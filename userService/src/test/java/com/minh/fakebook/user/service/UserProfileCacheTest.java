package com.minh.fakebook.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.minh.fakebook.user.config.ApplicationProperties;
import com.minh.fakebook.user.config.Constants;
import com.minh.fakebook.user.domain.UserProfile;
import com.minh.fakebook.user.repository.UserProfileRepository;
import com.minh.fakebook.user.service.dto.UserProfileCacheDTO;
import com.minh.fakebook.user.service.dto.UserProfileDTO;
import com.minh.fakebook.user.service.event.UserProfileChanged;
import com.minh.fakebook.user.service.mapper.UserProfileMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

class UserProfileCacheTest {

    private UserProfileRepository userProfileRepository;
    private UserProfileMapper userProfileMapper;
    private CacheManager cacheManager;
    private ApplicationProperties applicationProperties;
    private SimpleMeterRegistry meterRegistry;
    private Cache batchCache;
    private Cache profileCache;

    private UserProfileCache userProfileCache;

    @BeforeEach
    void setUp() {
        userProfileRepository = mock(UserProfileRepository.class);
        userProfileMapper = mock(UserProfileMapper.class);
        cacheManager = mock(CacheManager.class);
        batchCache = mock(Cache.class);
        profileCache = mock(Cache.class);
        applicationProperties = new ApplicationProperties();
        meterRegistry = new SimpleMeterRegistry();

        when(cacheManager.getCache(Constants.USER_PROFILE_CACHE)).thenReturn(batchCache);
        when(cacheManager.getCache("userProfile")).thenReturn(profileCache);

        userProfileCache = new UserProfileCache(
            userProfileRepository,
            userProfileMapper,
            cacheManager,
            applicationProperties,
            meterRegistry
        );
    }

    @Test
    void getProfilesReturnsFromCacheWhenPresent() {
        UUID id = UUID.randomUUID();
        UserProfileCacheDTO cached = new UserProfileCacheDTO(
            id,
            "john_doe",
            "John Doe",
            "Bio",
            "Hanoi",
            "University",
            "Tech",
            null,
            null,
            Instant.now(),
            Instant.now()
        );
        when(batchCache.get(id.toString(), UserProfileCacheDTO.class)).thenReturn(cached);

        Map<UUID, UserProfileDTO> result = userProfileCache.getProfiles(List.of(id));

        assertThat(result).hasSize(1);
        assertThat(result.get(id).getUsername()).isEqualTo("john_doe");
        assertThat(result.get(id).getDisplayName()).isEqualTo("John Doe");

        verify(userProfileRepository, never()).findAllById(any());
        assertThat(meterRegistry.counter("fakebook.cache.user_profile.hits").count()).isEqualTo(1.0);
        assertThat(meterRegistry.counter("fakebook.cache.user_profile.misses").count()).isEqualTo(0.0);
    }

    @Test
    void getProfilesLoadsFromDbOnMissAndPopulatesCache() {
        UUID id = UUID.randomUUID();
        when(batchCache.get(id.toString(), UserProfileCacheDTO.class)).thenReturn(null);

        UserProfile profile = new UserProfile();
        profile.setId(id);
        profile.setUsername("alice");
        profile.setDisplayName("Alice Wonderland");
        profile.setCreatedAt(Instant.now());

        UserProfileDTO dto = new UserProfileDTO();
        dto.setId(id);
        dto.setUsername("alice");
        dto.setDisplayName("Alice Wonderland");
        dto.setCreatedAt(profile.getCreatedAt());

        when(userProfileRepository.findAllById(Set.of(id))).thenReturn(List.of(profile));
        when(userProfileMapper.toDto(profile)).thenReturn(dto);

        Map<UUID, UserProfileDTO> result = userProfileCache.getProfiles(List.of(id));

        assertThat(result).hasSize(1);
        assertThat(result.get(id).getUsername()).isEqualTo("alice");

        verify(batchCache).put(eq(id.toString()), any(UserProfileCacheDTO.class));
        assertThat(meterRegistry.counter("fakebook.cache.user_profile.misses").count()).isEqualTo(1.0);
        assertThat(meterRegistry.counter("fakebook.cache.user_profile.hits").count()).isEqualTo(0.0);
    }

    @Test
    void getProfilesHandlesPartialMiss() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        UserProfileCacheDTO cached1 = new UserProfileCacheDTO(
            id1,
            "user1",
            "User 1",
            null,
            null,
            null,
            null,
            null,
            null,
            Instant.now(),
            Instant.now()
        );
        when(batchCache.get(id1.toString(), UserProfileCacheDTO.class)).thenReturn(cached1);
        when(batchCache.get(id2.toString(), UserProfileCacheDTO.class)).thenReturn(null);

        UserProfile profile2 = new UserProfile();
        profile2.setId(id2);
        profile2.setUsername("user2");
        profile2.setDisplayName("User 2");

        UserProfileDTO dto2 = new UserProfileDTO();
        dto2.setId(id2);
        dto2.setUsername("user2");
        dto2.setDisplayName("User 2");

        when(userProfileRepository.findAllById(Set.of(id2))).thenReturn(List.of(profile2));
        when(userProfileMapper.toDto(profile2)).thenReturn(dto2);

        Map<UUID, UserProfileDTO> result = userProfileCache.getProfiles(List.of(id1, id2));

        assertThat(result).hasSize(2);
        assertThat(result.get(id1).getUsername()).isEqualTo("user1");
        assertThat(result.get(id2).getUsername()).isEqualTo("user2");

        verify(batchCache).put(eq(id2.toString()), any(UserProfileCacheDTO.class));
        assertThat(meterRegistry.counter("fakebook.cache.user_profile.hits").count()).isEqualTo(1.0);
        assertThat(meterRegistry.counter("fakebook.cache.user_profile.misses").count()).isEqualTo(1.0);
    }

    @Test
    void getProfilesStoresEmptySentinelForNonExistentProfilesToPreventPenetration() {
        UUID nonExistentId = UUID.randomUUID();
        when(batchCache.get(nonExistentId.toString(), UserProfileCacheDTO.class)).thenReturn(null);
        when(userProfileRepository.findAllById(Set.of(nonExistentId))).thenReturn(List.of());

        Map<UUID, UserProfileDTO> result = userProfileCache.getProfiles(List.of(nonExistentId));

        assertThat(result).isEmpty();
        verify(batchCache).put(nonExistentId.toString(), UserProfileCacheDTO.EMPTY);
    }

    @Test
    void getProfilesTreatsEmptySentinelAsHitAndReturnsEmpty() {
        UUID nonExistentId = UUID.randomUUID();
        when(batchCache.get(nonExistentId.toString(), UserProfileCacheDTO.class)).thenReturn(UserProfileCacheDTO.EMPTY);

        Map<UUID, UserProfileDTO> result = userProfileCache.getProfiles(List.of(nonExistentId));

        assertThat(result).isEmpty();
        verify(userProfileRepository, never()).findAllById(any());
        assertThat(meterRegistry.counter("fakebook.cache.user_profile.hits").count()).isEqualTo(1.0);
    }

    @Test
    void getProfilesSurvivesCacheExceptionAndFallsBackToDb() {
        UUID id = UUID.randomUUID();
        when(batchCache.get(id.toString(), UserProfileCacheDTO.class)).thenThrow(new RuntimeException("Redis connection refused"));

        UserProfile profile = new UserProfile();
        profile.setId(id);
        profile.setUsername("alice");
        profile.setDisplayName("Alice");

        UserProfileDTO dto = new UserProfileDTO();
        dto.setId(id);
        dto.setUsername("alice");
        dto.setDisplayName("Alice");

        when(userProfileRepository.findAllById(Set.of(id))).thenReturn(List.of(profile));
        when(userProfileMapper.toDto(profile)).thenReturn(dto);

        Map<UUID, UserProfileDTO> result = userProfileCache.getProfiles(List.of(id));

        assertThat(result).hasSize(1);
        assertThat(result.get(id).getUsername()).isEqualTo("alice");
        assertThat(meterRegistry.counter("fakebook.cache.user_profile.errors", "operation", "get").count()).isEqualTo(1.0);
    }

    @Test
    void evictClearsBothBatchAndIndividualCacheKeys() {
        UUID userId = UUID.randomUUID();

        userProfileCache.evict(userId);

        verify(batchCache).evict(userId.toString());
        verify(profileCache).evict(userId.toString());
        verify(profileCache).evict(userId.toString() + ":optional");
    }

    @Test
    void evictionListenerDelegatesToUserProfileCache() {
        UserProfileEvictionListener listener = new UserProfileEvictionListener(userProfileCache);
        UUID userId = UUID.randomUUID();

        listener.onUserProfileChanged(new UserProfileChanged(userId));

        verify(batchCache).evict(userId.toString());
        verify(profileCache).evict(userId.toString());
        verify(profileCache).evict(userId.toString() + ":optional");
    }
}
