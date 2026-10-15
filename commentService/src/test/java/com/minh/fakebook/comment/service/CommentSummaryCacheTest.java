package com.minh.fakebook.comment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.minh.fakebook.comment.config.ApplicationProperties;
import com.minh.fakebook.comment.config.Constants;
import com.minh.fakebook.comment.service.dto.CommentPreviewDTO;
import com.minh.fakebook.comment.service.dto.CommentSummaryCacheDTO;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.cache.Cache;
import javax.cache.CacheManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CommentSummaryCacheTest {

    private CommentSummaryReader summaryReader;
    private CacheManager cacheManager;
    private Cache<UUID, CommentSummaryCacheDTO> cache;
    private ApplicationProperties applicationProperties;
    private SimpleMeterRegistry meterRegistry;
    private CommentSummaryCache summaryCache;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        summaryReader = mock(CommentSummaryReader.class);
        cacheManager = mock(CacheManager.class);
        cache = mock(Cache.class);
        applicationProperties = new ApplicationProperties();
        applicationProperties.getCache().getCommentSummary().setEnabled(true);
        meterRegistry = new SimpleMeterRegistry();

        when(cacheManager.getCache(Constants.COMMENT_SUMMARY_CACHE)).thenReturn((Cache) cache);

        summaryCache = new CommentSummaryCache(summaryReader, cacheManager, applicationProperties, meterRegistry);
    }

    @Test
    void shouldReturnEmptyMapWhenInputIsEmptyOrNull() {
        assertThat(summaryCache.getSummaries(null)).isEmpty();
        assertThat(summaryCache.getSummaries(List.of())).isEmpty();
        verifyNoInteractions(summaryReader);
        verify(cacheManager, never()).getCache(any());
    }

    @Test
    void shouldServeFromCacheOnHitWithoutCallingDatabase() {
        UUID postId = UUID.randomUUID();
        CommentSummaryCacheDTO cachedDto = new CommentSummaryCacheDTO(
            4L,
            new CommentPreviewDTO(UUID.randomUUID(), UUID.randomUUID(), "Nice post", Instant.now())
        );

        when(cache.getAll(Set.of(postId))).thenReturn(Map.of(postId, cachedDto));

        Map<UUID, CommentSummaryCacheDTO> result = summaryCache.getSummaries(List.of(postId));

        assertThat(result).containsEntry(postId, cachedDto);
        verifyNoInteractions(summaryReader);
        assertThat(meterRegistry.counter("fakebook.cache.comment_summary.hits").count()).isEqualTo(1.0);
        assertThat(meterRegistry.find("fakebook.cache.comment_summary.misses").counter()).isNull();
    }

    @Test
    void shouldFetchFromDatabaseAndPopulateCacheOnMiss() {
        UUID postId = UUID.randomUUID();
        CommentSummaryCacheDTO dbDto = new CommentSummaryCacheDTO(
            2L,
            new CommentPreviewDTO(UUID.randomUUID(), UUID.randomUUID(), "First comment", Instant.now())
        );

        when(cache.getAll(Set.of(postId))).thenReturn(Map.of());
        when(summaryReader.loadFromDatabase(Set.of(postId))).thenReturn(Map.of(postId, dbDto));

        Map<UUID, CommentSummaryCacheDTO> result = summaryCache.getSummaries(List.of(postId));

        assertThat(result).containsEntry(postId, dbDto);
        verify(summaryReader).loadFromDatabase(Set.of(postId));
        verify(cache).putAll(Map.of(postId, dbDto));
        assertThat(meterRegistry.counter("fakebook.cache.comment_summary.misses").count()).isEqualTo(1.0);
        assertThat(meterRegistry.find("fakebook.cache.comment_summary.hits").counter()).isNull();
    }

    @Test
    void shouldHandlePartialCacheMiss() {
        UUID cachedPostId = UUID.randomUUID();
        UUID missingPostId = UUID.randomUUID();

        CommentSummaryCacheDTO cachedDto = new CommentSummaryCacheDTO(1L, null);
        CommentSummaryCacheDTO dbDto = new CommentSummaryCacheDTO(3L, null);

        when(cache.getAll(Set.of(cachedPostId, missingPostId))).thenReturn(Map.of(cachedPostId, cachedDto));
        when(summaryReader.loadFromDatabase(Set.of(missingPostId))).thenReturn(Map.of(missingPostId, dbDto));

        Map<UUID, CommentSummaryCacheDTO> result = summaryCache.getSummaries(List.of(cachedPostId, missingPostId));

        assertThat(result).hasSize(2);
        assertThat(result.get(cachedPostId)).isEqualTo(cachedDto);
        assertThat(result.get(missingPostId)).isEqualTo(dbDto);

        verify(summaryReader).loadFromDatabase(Set.of(missingPostId));
        verify(cache).putAll(Map.of(missingPostId, dbDto));
        assertThat(meterRegistry.counter("fakebook.cache.comment_summary.hits").count()).isEqualTo(1.0);
        assertThat(meterRegistry.counter("fakebook.cache.comment_summary.misses").count()).isEqualTo(1.0);
    }

    @Test
    void shouldBypassCacheWhenDisabled() {
        applicationProperties.getCache().getCommentSummary().setEnabled(false);
        UUID postId = UUID.randomUUID();
        CommentSummaryCacheDTO dbDto = new CommentSummaryCacheDTO(0L, null);

        when(summaryReader.loadFromDatabase(Set.of(postId))).thenReturn(Map.of(postId, dbDto));

        Map<UUID, CommentSummaryCacheDTO> result = summaryCache.getSummaries(List.of(postId));

        assertThat(result).containsEntry(postId, dbDto);
        verify(cacheManager, never()).getCache(any());
        verify(summaryReader).loadFromDatabase(Set.of(postId));
    }

    @Test
    void shouldCacheZeroCountAndNullPreviewAsHit() {
        UUID postId = UUID.randomUUID();
        CommentSummaryCacheDTO emptyDto = new CommentSummaryCacheDTO(0L, null);

        when(cache.getAll(Set.of(postId))).thenReturn(Map.of(postId, emptyDto));

        Map<UUID, CommentSummaryCacheDTO> result = summaryCache.getSummaries(List.of(postId));

        assertThat(result).containsEntry(postId, emptyDto);
        verifyNoInteractions(summaryReader);
        assertThat(meterRegistry.counter("fakebook.cache.comment_summary.hits").count()).isEqualTo(1.0);
    }

    @Test
    void shouldFallbackToDatabaseWhenCacheGetThrowsException() {
        UUID postId = UUID.randomUUID();
        CommentSummaryCacheDTO dbDto = new CommentSummaryCacheDTO(5L, null);

        when(cache.getAll(anySet())).thenThrow(new RuntimeException("Redis connection refused"));
        when(summaryReader.loadFromDatabase(Set.of(postId))).thenReturn(Map.of(postId, dbDto));

        Map<UUID, CommentSummaryCacheDTO> result = summaryCache.getSummaries(List.of(postId));

        assertThat(result).containsEntry(postId, dbDto);
        verify(summaryReader).loadFromDatabase(Set.of(postId));
        assertThat(meterRegistry.counter("fakebook.cache.comment_summary.errors", "operation", "get").count()).isEqualTo(1.0);
    }

    @Test
    void shouldReturnDatabaseResultWhenCachePutThrowsException() {
        UUID postId = UUID.randomUUID();
        CommentSummaryCacheDTO dbDto = new CommentSummaryCacheDTO(2L, null);

        when(cache.getAll(Set.of(postId))).thenReturn(Map.of());
        when(summaryReader.loadFromDatabase(Set.of(postId))).thenReturn(Map.of(postId, dbDto));
        doThrow(new RuntimeException("Redis timeout")).when(cache).putAll(any());

        Map<UUID, CommentSummaryCacheDTO> result = summaryCache.getSummaries(List.of(postId));

        assertThat(result).containsEntry(postId, dbDto);
        assertThat(meterRegistry.counter("fakebook.cache.comment_summary.errors", "operation", "put").count()).isEqualTo(1.0);
    }

    @Test
    void shouldEvictKeySuccessfully() {
        UUID postId = UUID.randomUUID();

        summaryCache.evict(postId);

        verify(cache).remove(postId);
    }

    @Test
    void shouldNotThrowWhenCacheEvictThrowsException() {
        UUID postId = UUID.randomUUID();
        doThrow(new RuntimeException("Redis error on evict")).when(cache).remove(postId);

        summaryCache.evict(postId);

        assertThat(meterRegistry.counter("fakebook.cache.comment_summary.errors", "operation", "evict").count()).isEqualTo(1.0);
    }
}
