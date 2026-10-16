package com.minh.fakebook.post.service;

import com.minh.fakebook.post.config.ApplicationProperties;
import com.minh.fakebook.post.config.CacheConfiguration;
import com.minh.fakebook.post.service.dto.PostContentCacheDTO;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import javax.cache.Cache;
import javax.cache.CacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Cache-aside manager for post content and metadata.
 * Batches reads and writes, supports negative caching via EMPTY sentinel,
 * records metrics, and provides resilience against Redis outages.
 */
@Component
public class PostContentCache {

    private static final Logger LOG = LoggerFactory.getLogger(PostContentCache.class);

    private final PostContentReader postContentReader;
    private final CacheManager cacheManager;
    private final ApplicationProperties applicationProperties;
    private final MeterRegistry meterRegistry;

    public PostContentCache(
        PostContentReader postContentReader,
        CacheManager cacheManager,
        ApplicationProperties applicationProperties,
        MeterRegistry meterRegistry
    ) {
        this.postContentReader = postContentReader;
        this.cacheManager = cacheManager;
        this.applicationProperties = applicationProperties;
        this.meterRegistry = meterRegistry;
    }

    /**
     * Gets post content for given post IDs, reading from cache first and falling back to DB for misses.
     * Empty sentinels are filtered out from the returned map.
     *
     * @param postIds collection of post IDs
     * @return map of postId to non-empty PostContentCacheDTO
     */
    public Map<UUID, PostContentCacheDTO> getPosts(Collection<UUID> postIds) {
        if (postIds == null || postIds.isEmpty()) {
            return Map.of();
        }

        Set<UUID> distinctIds = new LinkedHashSet<>(postIds);

        if (!isCacheEnabled()) {
            return filterNonEmpty(postContentReader.loadFromDatabase(distinctIds));
        }

        Map<UUID, PostContentCacheDTO> cachedEntries = readFromCache(distinctIds);

        Map<UUID, PostContentCacheDTO> result = new HashMap<>();
        Set<UUID> missingIds = new HashSet<>();
        int hits = 0;
        int misses = 0;

        for (UUID id : distinctIds) {
            PostContentCacheDTO dto = cachedEntries != null ? cachedEntries.get(id) : null;
            if (dto != null) {
                if (!dto.isEmpty()) {
                    result.put(id, dto);
                }
                hits++;
            } else {
                missingIds.add(id);
                misses++;
            }
        }

        recordMetricCounts(hits, misses);

        if (!missingIds.isEmpty()) {
            Map<UUID, PostContentCacheDTO> loadedFromDb = loadFromDatabaseWithMetrics(missingIds);
            writeToCache(loadedFromDb);
            for (Map.Entry<UUID, PostContentCacheDTO> entry : loadedFromDb.entrySet()) {
                if (!entry.getValue().isEmpty()) {
                    result.put(entry.getKey(), entry.getValue());
                }
            }
        }

        return result;
    }

    /**
     * Gets a single post by ID.
     *
     * @param postId post ID
     * @return optional containing the post content, or empty if post doesn't exist
     */
    public Optional<PostContentCacheDTO> getPost(UUID postId) {
        if (postId == null) {
            return Optional.empty();
        }

        if (!isCacheEnabled()) {
            PostContentCacheDTO dto = postContentReader.loadFromDatabase(Set.of(postId)).get(postId);
            return dto != null && !dto.isEmpty() ? Optional.of(dto) : Optional.empty();
        }

        PostContentCacheDTO cached = readSingleFromCache(postId);
        if (cached != null) {
            recordMetricCounts(1, 0);
            return cached.isEmpty() ? Optional.empty() : Optional.of(cached);
        }

        recordMetricCounts(0, 1);
        Map<UUID, PostContentCacheDTO> loaded = loadFromDatabaseWithMetrics(Set.of(postId));
        writeToCache(loaded);
        PostContentCacheDTO dto = loaded.get(postId);
        return dto != null && !dto.isEmpty() ? Optional.of(dto) : Optional.empty();
    }

    /**
     * Evicts cached post content for a post.
     *
     * @param postId post ID to evict
     */
    public void evict(UUID postId) {
        if (postId == null) {
            return;
        }
        try {
            Cache<UUID, PostContentCacheDTO> cache = getCache();
            if (cache != null) {
                cache.remove(postId);
            }
        } catch (Exception ex) {
            LOG.warn("Failed to evict post content cache for post: {}", postId, ex);
            meterRegistry.counter("fakebook.cache.post_content.errors", "operation", "evict").increment();
        }
    }

    /**
     * Evicts cached post content for multiple posts.
     *
     * @param postIds collection of post IDs to evict
     */
    public void evictAll(Collection<UUID> postIds) {
        if (postIds == null || postIds.isEmpty()) {
            return;
        }
        try {
            Cache<UUID, PostContentCacheDTO> cache = getCache();
            if (cache != null) {
                cache.removeAll(new HashSet<>(postIds));
            }
        } catch (Exception ex) {
            LOG.warn("Failed to evict post content cache for posts: {}", postIds, ex);
            meterRegistry.counter("fakebook.cache.post_content.errors", "operation", "evict").increment();
        }
    }

    private Map<UUID, PostContentCacheDTO> readFromCache(Set<UUID> keys) {
        long start = System.nanoTime();
        try {
            Cache<UUID, PostContentCacheDTO> cache = getCache();
            if (cache != null) {
                return cache.getAll(keys);
            }
        } catch (Exception ex) {
            LOG.warn("Failed to read post content from cache, falling back to database", ex);
            meterRegistry.counter("fakebook.cache.post_content.errors", "operation", "get").increment();
        } finally {
            meterRegistry.timer("fakebook.cache.post_content.cache_duration").record(System.nanoTime() - start, TimeUnit.NANOSECONDS);
        }
        return Map.of();
    }

    private PostContentCacheDTO readSingleFromCache(UUID key) {
        long start = System.nanoTime();
        try {
            Cache<UUID, PostContentCacheDTO> cache = getCache();
            if (cache != null) {
                return cache.get(key);
            }
        } catch (Exception ex) {
            LOG.warn("Failed to read single post content from cache, falling back to database", ex);
            meterRegistry.counter("fakebook.cache.post_content.errors", "operation", "get").increment();
        } finally {
            meterRegistry.timer("fakebook.cache.post_content.cache_duration").record(System.nanoTime() - start, TimeUnit.NANOSECONDS);
        }
        return null;
    }

    private void writeToCache(Map<UUID, PostContentCacheDTO> entries) {
        if (entries == null || entries.isEmpty()) {
            return;
        }
        long start = System.nanoTime();
        try {
            Cache<UUID, PostContentCacheDTO> cache = getCache();
            if (cache != null) {
                cache.putAll(entries);
            }
        } catch (Exception ex) {
            LOG.warn("Failed to write post content to cache", ex);
            meterRegistry.counter("fakebook.cache.post_content.errors", "operation", "put").increment();
        } finally {
            meterRegistry.timer("fakebook.cache.post_content.cache_duration").record(System.nanoTime() - start, TimeUnit.NANOSECONDS);
        }
    }

    private Map<UUID, PostContentCacheDTO> loadFromDatabaseWithMetrics(Set<UUID> missingIds) {
        long start = System.nanoTime();
        try {
            return postContentReader.loadFromDatabase(missingIds);
        } finally {
            meterRegistry.timer("fakebook.cache.post_content.db_duration").record(System.nanoTime() - start, TimeUnit.NANOSECONDS);
        }
    }

    private Map<UUID, PostContentCacheDTO> filterNonEmpty(Map<UUID, PostContentCacheDTO> map) {
        Map<UUID, PostContentCacheDTO> filtered = new HashMap<>();
        for (Map.Entry<UUID, PostContentCacheDTO> entry : map.entrySet()) {
            if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                filtered.put(entry.getKey(), entry.getValue());
            }
        }
        return filtered;
    }

    private void recordMetricCounts(int hits, int misses) {
        if (hits > 0) {
            meterRegistry.counter("fakebook.cache.post_content.hits").increment(hits);
        }
        if (misses > 0) {
            meterRegistry.counter("fakebook.cache.post_content.misses").increment(misses);
        }
    }

    private boolean isCacheEnabled() {
        return applicationProperties.getCache().getPostContent().isEnabled();
    }

    @SuppressWarnings("unchecked")
    private Cache<UUID, PostContentCacheDTO> getCache() {
        if (cacheManager == null) {
            return null;
        }
        return (Cache<UUID, PostContentCacheDTO>) (Cache<?, ?>) cacheManager.getCache(CacheConfiguration.POST_CONTENT_CACHE);
    }
}
