package com.minh.fakebook.user.service;

import com.minh.fakebook.user.config.ApplicationProperties;
import com.minh.fakebook.user.config.Constants;
import com.minh.fakebook.user.domain.UserProfile;
import com.minh.fakebook.user.repository.UserProfileRepository;
import com.minh.fakebook.user.service.dto.UserProfileCacheDTO;
import com.minh.fakebook.user.service.dto.UserProfileDTO;
import com.minh.fakebook.user.service.mapper.UserProfileMapper;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

/**
 * Cache-aside manager for batch user profiles.
 * Eliminates redundant database queries during feed hydration,
 * protects against penetration attacks, records Micrometer metrics,
 * and survives cache manager outages gracefully.
 */
@Component
public class UserProfileCache {

    private static final Logger LOG = LoggerFactory.getLogger(UserProfileCache.class);

    private final UserProfileRepository userProfileRepository;
    private final UserProfileMapper userProfileMapper;
    private final CacheManager cacheManager;
    private final ApplicationProperties applicationProperties;
    private final MeterRegistry meterRegistry;

    public UserProfileCache(
        UserProfileRepository userProfileRepository,
        UserProfileMapper userProfileMapper,
        CacheManager cacheManager,
        ApplicationProperties applicationProperties,
        MeterRegistry meterRegistry
    ) {
        this.userProfileRepository = userProfileRepository;
        this.userProfileMapper = userProfileMapper;
        this.cacheManager = cacheManager;
        this.applicationProperties = applicationProperties;
        this.meterRegistry = meterRegistry;
    }

    /**
     * Gets user profiles for given IDs, checking cache first and falling back to DB in a single batch.
     *
     * @param userIds collection of user UUIDs
     * @return map of UUID to UserProfileDTO
     */
    public Map<UUID, UserProfileDTO> getProfiles(Collection<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }

        Set<UUID> distinctIds = new LinkedHashSet<>(userIds);

        if (!isCacheEnabled()) {
            return loadFromDatabaseDirectly(distinctIds);
        }

        Map<UUID, UserProfileDTO> result = new HashMap<>();
        Set<UUID> missingIds = new HashSet<>();
        int hits = 0;
        int misses = 0;

        Cache batchCache = getBatchCache();

        long cacheStart = System.nanoTime();
        try {
            for (UUID id : distinctIds) {
                UserProfileCacheDTO cached = null;
                try {
                    cached = batchCache != null ? batchCache.get(id.toString(), UserProfileCacheDTO.class) : null;
                } catch (Exception ex) {
                    LOG.warn("Failed to get profile from cache for id {}: {}", id, ex.getMessage());
                    meterRegistry.counter("fakebook.cache.user_profile.errors", "operation", "get").increment();
                }

                if (cached != null) {
                    if (!cached.isEmpty()) {
                        result.put(id, cached.toDTO());
                    }
                    hits++;
                } else {
                    missingIds.add(id);
                    misses++;
                }
            }
        } finally {
            meterRegistry.timer("fakebook.cache.user_profile.cache_duration").record(System.nanoTime() - cacheStart, TimeUnit.NANOSECONDS);
        }

        recordMetricCounts(hits, misses);

        if (!missingIds.isEmpty()) {
            Map<UUID, UserProfileDTO> loadedFromDb = loadFromDatabaseWithMetrics(missingIds);
            result.putAll(loadedFromDb);
            writeToCache(batchCache, missingIds, loadedFromDb);
        }

        return result;
    }

    /**
     * Evicts cached user profile across batch and individual cache regions.
     *
     * @param userId user UUID to evict
     */
    public void evict(UUID userId) {
        if (userId == null) {
            return;
        }

        try {
            Cache batchCache = getBatchCache();
            if (batchCache != null) {
                batchCache.evict(userId.toString());
            }
        } catch (Exception ex) {
            LOG.warn("Failed to evict batch user profile cache for user {}: {}", userId, ex.getMessage());
            meterRegistry.counter("fakebook.cache.user_profile.errors", "operation", "evict").increment();
        }

        try {
            Cache userProfileCache = getUserProfileCache();
            if (userProfileCache != null) {
                userProfileCache.evict(userId.toString());
                userProfileCache.evict(userId.toString() + ":optional");
            }
        } catch (Exception ex) {
            LOG.warn("Failed to evict userProfile cache for user {}: {}", userId, ex.getMessage());
            meterRegistry.counter("fakebook.cache.user_profile.errors", "operation", "evict").increment();
        }
    }

    private Map<UUID, UserProfileDTO> loadFromDatabaseDirectly(Set<UUID> ids) {
        List<UserProfile> profiles = userProfileRepository.findAllById(ids);
        Map<UUID, UserProfileDTO> result = new HashMap<>();
        for (UserProfile profile : profiles) {
            UserProfileDTO dto = userProfileMapper.toDto(profile);
            result.put(dto.getId(), dto);
        }
        return result;
    }

    private Map<UUID, UserProfileDTO> loadFromDatabaseWithMetrics(Set<UUID> missingIds) {
        long start = System.nanoTime();
        try {
            return loadFromDatabaseDirectly(missingIds);
        } finally {
            meterRegistry.timer("fakebook.cache.user_profile.db_duration").record(System.nanoTime() - start, TimeUnit.NANOSECONDS);
        }
    }

    private void writeToCache(Cache cache, Set<UUID> requestedIds, Map<UUID, UserProfileDTO> loaded) {
        if (cache == null) {
            return;
        }

        long start = System.nanoTime();
        try {
            for (Map.Entry<UUID, UserProfileDTO> entry : loaded.entrySet()) {
                try {
                    cache.put(entry.getKey().toString(), UserProfileCacheDTO.fromDTO(entry.getValue()));
                } catch (Exception ex) {
                    LOG.warn("Failed to write user profile to cache for id {}: {}", entry.getKey(), ex.getMessage());
                    meterRegistry.counter("fakebook.cache.user_profile.errors", "operation", "put").increment();
                }
            }

            // Negative caching for non-existent profiles (protection against cache penetration)
            for (UUID id : requestedIds) {
                if (!loaded.containsKey(id)) {
                    try {
                        cache.put(id.toString(), UserProfileCacheDTO.EMPTY);
                    } catch (Exception ex) {
                        LOG.warn("Failed to write empty sentinel to cache for id {}: {}", id, ex.getMessage());
                        meterRegistry.counter("fakebook.cache.user_profile.errors", "operation", "put").increment();
                    }
                }
            }
        } finally {
            meterRegistry.timer("fakebook.cache.user_profile.cache_duration").record(System.nanoTime() - start, TimeUnit.NANOSECONDS);
        }
    }

    private void recordMetricCounts(int hits, int misses) {
        if (hits > 0) {
            meterRegistry.counter("fakebook.cache.user_profile.hits").increment(hits);
        }
        if (misses > 0) {
            meterRegistry.counter("fakebook.cache.user_profile.misses").increment(misses);
        }
    }

    private boolean isCacheEnabled() {
        return applicationProperties.getCache().getUserProfile().isEnabled();
    }

    private Cache getBatchCache() {
        if (cacheManager == null) {
            return null;
        }
        return cacheManager.getCache(Constants.USER_PROFILE_CACHE);
    }

    private Cache getUserProfileCache() {
        if (cacheManager == null) {
            return null;
        }
        return cacheManager.getCache("userProfile");
    }
}
