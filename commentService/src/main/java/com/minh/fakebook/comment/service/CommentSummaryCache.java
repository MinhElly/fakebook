package com.minh.fakebook.comment.service;

import com.minh.fakebook.comment.config.ApplicationProperties;
import com.minh.fakebook.comment.config.Constants;
import com.minh.fakebook.comment.service.dto.CommentSummaryCacheDTO;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Collection;
import java.util.Collections;
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
 * Cache-aside manager for comment summaries.
 * Batches reads and writes, records metrics, and provides resilience against Redis outages.
 */
@Component
public class CommentSummaryCache {

    private static final Logger LOG = LoggerFactory.getLogger(CommentSummaryCache.class);

    private final CommentSummaryReader summaryReader;
    private final CacheManager cacheManager;
    private final ApplicationProperties applicationProperties;
    private final MeterRegistry meterRegistry;

    public CommentSummaryCache(
        CommentSummaryReader summaryReader,
        CacheManager cacheManager,
        ApplicationProperties applicationProperties,
        MeterRegistry meterRegistry
    ) {
        this.summaryReader = summaryReader;
        this.cacheManager = cacheManager;
        this.applicationProperties = applicationProperties;
        this.meterRegistry = meterRegistry;
    }

    /**
     * Gets comment summaries for given post IDs, reading from cache first and falling back to DB for misses.
     *
     * @param postIds collection of post IDs
     * @return map of postId to CommentSummaryCacheDTO
     */
    public Map<UUID, CommentSummaryCacheDTO> getSummaries(Collection<UUID> postIds) {
        if (postIds == null || postIds.isEmpty()) {
            return Map.of();
        }

        Set<UUID> distinctIds = new LinkedHashSet<>(postIds);

        if (!isCacheEnabled()) {
            return summaryReader.loadFromDatabase(distinctIds);
        }

        Map<UUID, CommentSummaryCacheDTO> cachedEntries = readFromCache(distinctIds);

        Map<UUID, CommentSummaryCacheDTO> result = new HashMap<>();
        Set<UUID> missingIds = new HashSet<>();
        int hits = 0;
        int misses = 0;

        for (UUID id : distinctIds) {
            CommentSummaryCacheDTO dto = cachedEntries != null ? cachedEntries.get(id) : null;
            if (dto != null) {
                result.put(id, dto);
                hits++;
            } else {
                missingIds.add(id);
                misses++;
            }
        }

        recordMetricCounts(hits, misses);

        if (!missingIds.isEmpty()) {
            Map<UUID, CommentSummaryCacheDTO> loadedFromDb = loadFromDatabaseWithMetrics(missingIds);
            result.putAll(loadedFromDb);
            writeToCache(loadedFromDb);
        }

        return result;
    }

    /**
     * Evicts cached comment summary for a post.
     *
     * @param postId post ID to evict
     */
    public void evict(UUID postId) {
        if (postId == null) {
            return;
        }
        try {
            Cache<UUID, CommentSummaryCacheDTO> cache = getCache();
            if (cache != null) {
                cache.remove(postId);
            }
        } catch (Exception ex) {
            LOG.warn("Failed to evict comment summary cache for post: {}", postId, ex);
            meterRegistry.counter("fakebook.cache.comment_summary.errors", "operation", "evict").increment();
        }
    }

    private Map<UUID, CommentSummaryCacheDTO> readFromCache(Set<UUID> keys) {
        long start = System.nanoTime();
        try {
            Cache<UUID, CommentSummaryCacheDTO> cache = getCache();
            if (cache != null) {
                return cache.getAll(keys);
            }
        } catch (Exception ex) {
            LOG.warn("Failed to read comment summary from cache, falling back to database", ex);
            meterRegistry.counter("fakebook.cache.comment_summary.errors", "operation", "get").increment();
        } finally {
            meterRegistry.timer("fakebook.cache.comment_summary.cache_duration").record(System.nanoTime() - start, TimeUnit.NANOSECONDS);
        }
        return Map.of();
    }

    private void writeToCache(Map<UUID, CommentSummaryCacheDTO> entries) {
        if (entries == null || entries.isEmpty()) {
            return;
        }
        long start = System.nanoTime();
        try {
            Cache<UUID, CommentSummaryCacheDTO> cache = getCache();
            if (cache != null) {
                cache.putAll(entries);
            }
        } catch (Exception ex) {
            LOG.warn("Failed to write comment summary to cache", ex);
            meterRegistry.counter("fakebook.cache.comment_summary.errors", "operation", "put").increment();
        } finally {
            meterRegistry.timer("fakebook.cache.comment_summary.cache_duration").record(System.nanoTime() - start, TimeUnit.NANOSECONDS);
        }
    }

    private Map<UUID, CommentSummaryCacheDTO> loadFromDatabaseWithMetrics(Set<UUID> missingIds) {
        long start = System.nanoTime();
        try {
            return summaryReader.loadFromDatabase(missingIds);
        } finally {
            meterRegistry.timer("fakebook.cache.comment_summary.db_duration").record(System.nanoTime() - start, TimeUnit.NANOSECONDS);
        }
    }

    private void recordMetricCounts(int hits, int misses) {
        if (hits > 0) {
            meterRegistry.counter("fakebook.cache.comment_summary.hits").increment(hits);
        }
        if (misses > 0) {
            meterRegistry.counter("fakebook.cache.comment_summary.misses").increment(misses);
        }
    }

    private boolean isCacheEnabled() {
        return applicationProperties.getCache().getCommentSummary().isEnabled();
    }

    @SuppressWarnings("unchecked")
    private Cache<UUID, CommentSummaryCacheDTO> getCache() {
        if (cacheManager == null) {
            return null;
        }
        return (Cache<UUID, CommentSummaryCacheDTO>) (Cache<?, ?>) cacheManager.getCache(Constants.COMMENT_SUMMARY_CACHE);
    }
}
