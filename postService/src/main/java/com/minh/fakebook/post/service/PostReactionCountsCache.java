package com.minh.fakebook.post.service;

import com.minh.fakebook.post.config.ApplicationProperties;
import com.minh.fakebook.post.config.CacheConfiguration;
import com.minh.fakebook.post.domain.enumeration.ReactionType;
import com.minh.fakebook.post.service.dto.ReactionCountsCacheDTO;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import javax.cache.Cache;
import javax.cache.CacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Cache-aside manager for post reaction counts.
 * Batches reads and writes, records metrics, and provides resilience against Redis outages.
 */
@Component
public class PostReactionCountsCache {

    private static final Logger LOG = LoggerFactory.getLogger(PostReactionCountsCache.class);

    private final PostReactionCountsReader countsReader;
    private final CacheManager cacheManager;
    private final ApplicationProperties applicationProperties;
    private final MeterRegistry meterRegistry;

    public PostReactionCountsCache(
        PostReactionCountsReader countsReader,
        CacheManager cacheManager,
        ApplicationProperties applicationProperties,
        MeterRegistry meterRegistry
    ) {
        this.countsReader = countsReader;
        this.cacheManager = cacheManager;
        this.applicationProperties = applicationProperties;
        this.meterRegistry = meterRegistry;
    }

    /**
     * Gets reaction counts for given post IDs, reading from cache first and falling back to DB for misses.
     *
     * @param postIds collection of post IDs
     * @return map of postId to reaction counts map
     */
    public Map<UUID, Map<ReactionType, Long>> getCounts(Collection<UUID> postIds) {
        if (postIds == null || postIds.isEmpty()) {
            return Map.of();
        }

        Set<UUID> distinctIds = new LinkedHashSet<>(postIds);

        if (!isCacheEnabled()) {
            return countsReader.loadFromDatabase(distinctIds);
        }

        Map<UUID, ReactionCountsCacheDTO> cachedEntries = readFromCache(distinctIds);

        Map<UUID, Map<ReactionType, Long>> result = new HashMap<>();
        Set<UUID> missingIds = new HashSet<>();
        int hits = 0;
        int misses = 0;

        for (UUID id : distinctIds) {
            ReactionCountsCacheDTO dto = cachedEntries != null ? cachedEntries.get(id) : null;
            if (dto != null && dto.counts() != null) {
                result.put(id, new EnumMap<>(dto.counts()));
                hits++;
            } else {
                missingIds.add(id);
                misses++;
            }
        }

        recordMetricCounts(hits, misses);

        if (!missingIds.isEmpty()) {
            Map<UUID, Map<ReactionType, Long>> loadedFromDb = loadFromDatabaseWithMetrics(missingIds);
            result.putAll(loadedFromDb);

            Map<UUID, ReactionCountsCacheDTO> toCache = new HashMap<>();
            for (Map.Entry<UUID, Map<ReactionType, Long>> entry : loadedFromDb.entrySet()) {
                toCache.put(entry.getKey(), new ReactionCountsCacheDTO(entry.getValue()));
            }
            writeToCache(toCache);
        }

        return result;
    }

    /**
     * Evicts cached reaction counts for a post.
     *
     * @param postId post ID to evict
     */
    public void evict(UUID postId) {
        if (postId == null) {
            return;
        }
        try {
            Cache<UUID, ReactionCountsCacheDTO> cache = getCache();
            if (cache != null) {
                cache.remove(postId);
            }
        } catch (Exception ex) {
            LOG.warn("Failed to evict reaction counts cache for post: {}", postId, ex);
            meterRegistry.counter("fakebook.cache.reaction_counts.errors", "operation", "evict").increment();
        }
    }

    private Map<UUID, ReactionCountsCacheDTO> readFromCache(Set<UUID> keys) {
        long start = System.nanoTime();
        try {
            Cache<UUID, ReactionCountsCacheDTO> cache = getCache();
            if (cache != null) {
                return cache.getAll(keys);
            }
        } catch (Exception ex) {
            LOG.warn("Failed to read reaction counts from cache, falling back to database", ex);
            meterRegistry.counter("fakebook.cache.reaction_counts.errors", "operation", "get").increment();
        } finally {
            meterRegistry.timer("fakebook.cache.reaction_counts.cache_duration").record(System.nanoTime() - start, TimeUnit.NANOSECONDS);
        }
        return Map.of();
    }

    private void writeToCache(Map<UUID, ReactionCountsCacheDTO> entries) {
        if (entries == null || entries.isEmpty()) {
            return;
        }
        long start = System.nanoTime();
        try {
            Cache<UUID, ReactionCountsCacheDTO> cache = getCache();
            if (cache != null) {
                cache.putAll(entries);
            }
        } catch (Exception ex) {
            LOG.warn("Failed to write reaction counts to cache", ex);
            meterRegistry.counter("fakebook.cache.reaction_counts.errors", "operation", "put").increment();
        } finally {
            meterRegistry.timer("fakebook.cache.reaction_counts.cache_duration").record(System.nanoTime() - start, TimeUnit.NANOSECONDS);
        }
    }

    private Map<UUID, Map<ReactionType, Long>> loadFromDatabaseWithMetrics(Set<UUID> missingIds) {
        long start = System.nanoTime();
        try {
            return countsReader.loadFromDatabase(missingIds);
        } finally {
            meterRegistry.timer("fakebook.cache.reaction_counts.db_duration").record(System.nanoTime() - start, TimeUnit.NANOSECONDS);
        }
    }

    private void recordMetricCounts(int hits, int misses) {
        if (hits > 0) {
            meterRegistry.counter("fakebook.cache.reaction_counts.hits").increment(hits);
        }
        if (misses > 0) {
            meterRegistry.counter("fakebook.cache.reaction_counts.misses").increment(misses);
        }
    }

    private boolean isCacheEnabled() {
        return applicationProperties.getCache().getReactionCounts().isEnabled();
    }

    @SuppressWarnings("unchecked")
    private Cache<UUID, ReactionCountsCacheDTO> getCache() {
        if (cacheManager == null) {
            return null;
        }
        return (Cache<UUID, ReactionCountsCacheDTO>) (Cache<?, ?>) cacheManager.getCache(CacheConfiguration.POST_REACTION_COUNTS_CACHE);
    }
}
