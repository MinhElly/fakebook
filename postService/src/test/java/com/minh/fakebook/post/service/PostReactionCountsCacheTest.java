package com.minh.fakebook.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.minh.fakebook.post.config.ApplicationProperties;
import com.minh.fakebook.post.config.CacheConfiguration;
import com.minh.fakebook.post.domain.enumeration.ReactionType;
import com.minh.fakebook.post.service.dto.ReactionCountsCacheDTO;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.cache.Cache;
import javax.cache.CacheManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PostReactionCountsCacheTest {

    private PostReactionCountsReader countsReader;
    private CacheManager cacheManager;
    private Cache<UUID, ReactionCountsCacheDTO> cache;
    private ApplicationProperties properties;
    private SimpleMeterRegistry meterRegistry;
    private PostReactionCountsCache countsCache;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        countsReader = mock(PostReactionCountsReader.class);
        cacheManager = mock(CacheManager.class);
        cache = mock(Cache.class);
        properties = new ApplicationProperties();
        meterRegistry = new SimpleMeterRegistry();

        when(cacheManager.getCache(CacheConfiguration.POST_REACTION_COUNTS_CACHE)).thenReturn((Cache) cache);

        countsCache = new PostReactionCountsCache(countsReader, cacheManager, properties, meterRegistry);
    }

    @Test
    void getCounts_withEmptyCollection_returnsEmptyMap() {
        assertThat(countsCache.getCounts(null)).isEmpty();
        assertThat(countsCache.getCounts(List.of())).isEmpty();
        verifyNoInteractions(countsReader);
    }

    @Test
    void getCounts_whenCacheDisabled_delegatesDirectlyToReader() {
        properties.getCache().getReactionCounts().setEnabled(false);
        UUID post1 = UUID.randomUUID();

        Map<ReactionType, Long> counts1 = emptyCounts();
        counts1.put(ReactionType.LIKE, 3L);
        when(countsReader.loadFromDatabase(any())).thenReturn(Map.of(post1, counts1));

        Map<UUID, Map<ReactionType, Long>> result = countsCache.getCounts(List.of(post1));

        assertThat(result).containsKey(post1);
        assertThat(result.get(post1).get(ReactionType.LIKE)).isEqualTo(3L);
        verifyNoInteractions(cache);
    }

    @Test
    void getCounts_fullMiss_loadsFromReaderAndCaches() {
        UUID post1 = UUID.randomUUID();
        when(cache.getAll(Set.of(post1))).thenReturn(Map.of());

        Map<ReactionType, Long> dbCounts = emptyCounts();
        dbCounts.put(ReactionType.LOVE, 5L);
        when(countsReader.loadFromDatabase(Set.of(post1))).thenReturn(Map.of(post1, dbCounts));

        Map<UUID, Map<ReactionType, Long>> result = countsCache.getCounts(List.of(post1));

        assertThat(result).containsKey(post1);
        assertThat(result.get(post1).get(ReactionType.LOVE)).isEqualTo(5L);
        verify(cache).putAll(anyMap());
        assertThat(meterRegistry.counter("fakebook.cache.reaction_counts.misses").count()).isEqualTo(1.0);
        assertThat(meterRegistry.counter("fakebook.cache.reaction_counts.hits").count()).isEqualTo(0.0);
    }

    @Test
    void getCounts_fullHit_doesNotCallReader() {
        UUID post1 = UUID.randomUUID();
        Map<ReactionType, Long> cachedMap = emptyCounts();
        cachedMap.put(ReactionType.LIKE, 12L);
        ReactionCountsCacheDTO dto = new ReactionCountsCacheDTO(cachedMap);

        when(cache.getAll(Set.of(post1))).thenReturn(Map.of(post1, dto));

        Map<UUID, Map<ReactionType, Long>> result = countsCache.getCounts(List.of(post1));

        assertThat(result).containsKey(post1);
        assertThat(result.get(post1).get(ReactionType.LIKE)).isEqualTo(12L);
        verify(countsReader, never()).loadFromDatabase(any());
        assertThat(meterRegistry.counter("fakebook.cache.reaction_counts.hits").count()).isEqualTo(1.0);
    }

    @Test
    void getCounts_partialHit_loadsOnlyMissingFromReader() {
        UUID postHit = UUID.randomUUID();
        UUID postMiss = UUID.randomUUID();

        Map<ReactionType, Long> cachedMap = emptyCounts();
        cachedMap.put(ReactionType.LIKE, 10L);
        ReactionCountsCacheDTO dto = new ReactionCountsCacheDTO(cachedMap);

        when(cache.getAll(Set.of(postHit, postMiss))).thenReturn(Map.of(postHit, dto));

        Map<ReactionType, Long> dbMap = emptyCounts();
        dbMap.put(ReactionType.ANGRY, 4L);
        when(countsReader.loadFromDatabase(Set.of(postMiss))).thenReturn(Map.of(postMiss, dbMap));

        Map<UUID, Map<ReactionType, Long>> result = countsCache.getCounts(List.of(postHit, postMiss));

        assertThat(result).containsOnlyKeys(postHit, postMiss);
        assertThat(result.get(postHit).get(ReactionType.LIKE)).isEqualTo(10L);
        assertThat(result.get(postMiss).get(ReactionType.ANGRY)).isEqualTo(4L);

        verify(countsReader).loadFromDatabase(Set.of(postMiss));
        verify(cache).putAll(anyMap());
        assertThat(meterRegistry.counter("fakebook.cache.reaction_counts.hits").count()).isEqualTo(1.0);
        assertThat(meterRegistry.counter("fakebook.cache.reaction_counts.misses").count()).isEqualTo(1.0);
    }

    @Test
    void getCounts_whenCacheThrowsExceptionOnGet_fallsBackToReader() {
        UUID post1 = UUID.randomUUID();
        when(cache.getAll(any())).thenThrow(new RuntimeException("Redis connection refused"));

        Map<ReactionType, Long> dbMap = emptyCounts();
        dbMap.put(ReactionType.HAHA, 2L);
        when(countsReader.loadFromDatabase(Set.of(post1))).thenReturn(Map.of(post1, dbMap));

        Map<UUID, Map<ReactionType, Long>> result = countsCache.getCounts(List.of(post1));

        assertThat(result).containsKey(post1);
        assertThat(result.get(post1).get(ReactionType.HAHA)).isEqualTo(2L);
        assertThat(meterRegistry.counter("fakebook.cache.reaction_counts.errors", "operation", "get").count()).isEqualTo(1.0);
    }

    @Test
    void getCounts_whenCacheThrowsExceptionOnPut_returnsDbResultWithoutFailing() {
        UUID post1 = UUID.randomUUID();
        when(cache.getAll(Set.of(post1))).thenReturn(Map.of());

        Map<ReactionType, Long> dbMap = emptyCounts();
        dbMap.put(ReactionType.SAD, 1L);
        when(countsReader.loadFromDatabase(Set.of(post1))).thenReturn(Map.of(post1, dbMap));
        doThrow(new RuntimeException("Redis put timeout")).when(cache).putAll(anyMap());

        Map<UUID, Map<ReactionType, Long>> result = countsCache.getCounts(List.of(post1));

        assertThat(result).containsKey(post1);
        assertThat(result.get(post1).get(ReactionType.SAD)).isEqualTo(1L);
        assertThat(meterRegistry.counter("fakebook.cache.reaction_counts.errors", "operation", "put").count()).isEqualTo(1.0);
    }

    @Test
    void evict_callsCacheRemove() {
        UUID postId = UUID.randomUUID();
        countsCache.evict(postId);
        verify(cache).remove(postId);
    }

    @Test
    void evict_whenCacheThrowsException_handlesGracefullyAndRecordsMetric() {
        UUID postId = UUID.randomUUID();
        doThrow(new RuntimeException("Redis down")).when(cache).remove(postId);

        countsCache.evict(postId);

        assertThat(meterRegistry.counter("fakebook.cache.reaction_counts.errors", "operation", "evict").count()).isEqualTo(1.0);
    }

    private Map<ReactionType, Long> emptyCounts() {
        EnumMap<ReactionType, Long> map = new EnumMap<>(ReactionType.class);
        for (ReactionType type : ReactionType.values()) {
            map.put(type, 0L);
        }
        return map;
    }
}
