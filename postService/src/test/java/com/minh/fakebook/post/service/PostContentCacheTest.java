package com.minh.fakebook.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.minh.fakebook.post.config.ApplicationProperties;
import com.minh.fakebook.post.config.CacheConfiguration;
import com.minh.fakebook.post.domain.enumeration.PostStatus;
import com.minh.fakebook.post.domain.enumeration.PostVisibility;
import com.minh.fakebook.post.service.dto.PostContentCacheDTO;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.cache.Cache;
import javax.cache.CacheManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PostContentCacheTest {

    private PostContentReader contentReader;
    private CacheManager cacheManager;
    private Cache<UUID, PostContentCacheDTO> cache;
    private ApplicationProperties properties;
    private SimpleMeterRegistry meterRegistry;
    private PostContentCache contentCache;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        contentReader = mock(PostContentReader.class);
        cacheManager = mock(CacheManager.class);
        cache = mock(Cache.class);
        properties = new ApplicationProperties();
        meterRegistry = new SimpleMeterRegistry();

        when(cacheManager.getCache(CacheConfiguration.POST_CONTENT_CACHE)).thenReturn((Cache) cache);

        contentCache = new PostContentCache(contentReader, cacheManager, properties, meterRegistry);
    }

    private PostContentCacheDTO samplePost(UUID id, String content) {
        return new PostContentCacheDTO(
            id,
            UUID.randomUUID(),
            content,
            PostVisibility.PUBLIC,
            PostStatus.ACTIVE,
            Instant.now(),
            Instant.now(),
            List.of(),
            Set.of(),
            1L
        );
    }

    @Test
    void getPosts_withEmptyCollection_returnsEmptyMap() {
        assertThat(contentCache.getPosts(null)).isEmpty();
        assertThat(contentCache.getPosts(List.of())).isEmpty();
        verifyNoInteractions(contentReader);
    }

    @Test
    void getPosts_whenCacheDisabled_delegatesDirectlyToReader() {
        properties.getCache().getPostContent().setEnabled(false);
        UUID post1 = UUID.randomUUID();
        PostContentCacheDTO dto = samplePost(post1, "Hello World");

        when(contentReader.loadFromDatabase(any())).thenReturn(Map.of(post1, dto));

        Map<UUID, PostContentCacheDTO> result = contentCache.getPosts(List.of(post1));

        assertThat(result).containsKey(post1);
        assertThat(result.get(post1).content()).isEqualTo("Hello World");
        verifyNoInteractions(cache);
    }

    @Test
    void getPosts_fullMiss_loadsFromReaderAndCaches() {
        UUID post1 = UUID.randomUUID();
        when(cache.getAll(Set.of(post1))).thenReturn(Map.of());

        PostContentCacheDTO dto = samplePost(post1, "New Post");
        when(contentReader.loadFromDatabase(Set.of(post1))).thenReturn(Map.of(post1, dto));

        Map<UUID, PostContentCacheDTO> result = contentCache.getPosts(List.of(post1));

        assertThat(result).containsKey(post1);
        assertThat(result.get(post1).content()).isEqualTo("New Post");
        verify(cache).putAll(anyMap());
        assertThat(meterRegistry.counter("fakebook.cache.post_content.misses").count()).isEqualTo(1.0);
        assertThat(meterRegistry.counter("fakebook.cache.post_content.hits").count()).isEqualTo(0.0);
    }

    @Test
    void getPosts_fullHit_doesNotCallReader() {
        UUID post1 = UUID.randomUUID();
        PostContentCacheDTO dto = samplePost(post1, "Cached Post");

        when(cache.getAll(Set.of(post1))).thenReturn(Map.of(post1, dto));

        Map<UUID, PostContentCacheDTO> result = contentCache.getPosts(List.of(post1));

        assertThat(result).containsKey(post1);
        assertThat(result.get(post1).content()).isEqualTo("Cached Post");
        verifyNoInteractions(contentReader);
        assertThat(meterRegistry.counter("fakebook.cache.post_content.hits").count()).isEqualTo(1.0);
        assertThat(meterRegistry.counter("fakebook.cache.post_content.misses").count()).isEqualTo(0.0);
    }

    @Test
    void getPosts_negativeCaching_filtersOutEmptySentinelAndRecordsHit() {
        UUID nonExistentId = UUID.randomUUID();
        when(cache.getAll(Set.of(nonExistentId))).thenReturn(Map.of(nonExistentId, PostContentCacheDTO.EMPTY));

        Map<UUID, PostContentCacheDTO> result = contentCache.getPosts(List.of(nonExistentId));

        assertThat(result).isEmpty();
        verifyNoInteractions(contentReader);
        assertThat(meterRegistry.counter("fakebook.cache.post_content.hits").count()).isEqualTo(1.0);
    }

    @Test
    void getPosts_whenCacheFailsOnGet_fallsBackToReaderAndRecordsMetric() {
        UUID post1 = UUID.randomUUID();
        when(cache.getAll(any())).thenThrow(new RuntimeException("Redis connection refused"));

        PostContentCacheDTO dto = samplePost(post1, "DB Post");
        when(contentReader.loadFromDatabase(Set.of(post1))).thenReturn(Map.of(post1, dto));

        Map<UUID, PostContentCacheDTO> result = contentCache.getPosts(List.of(post1));

        assertThat(result).containsKey(post1);
        assertThat(result.get(post1).content()).isEqualTo("DB Post");
        assertThat(meterRegistry.counter("fakebook.cache.post_content.errors", "operation", "get").count()).isEqualTo(1.0);
    }

    @Test
    void getPosts_whenCacheFailsOnPut_returnsDbResultsAndRecordsMetric() {
        UUID post1 = UUID.randomUUID();
        when(cache.getAll(Set.of(post1))).thenReturn(Map.of());

        PostContentCacheDTO dto = samplePost(post1, "DB Post");
        when(contentReader.loadFromDatabase(Set.of(post1))).thenReturn(Map.of(post1, dto));
        doThrow(new RuntimeException("Redis write timeout")).when(cache).putAll(anyMap());

        Map<UUID, PostContentCacheDTO> result = contentCache.getPosts(List.of(post1));

        assertThat(result).containsKey(post1);
        assertThat(result.get(post1).content()).isEqualTo("DB Post");
        assertThat(meterRegistry.counter("fakebook.cache.post_content.errors", "operation", "put").count()).isEqualTo(1.0);
    }

    @Test
    void getPost_fullHit_returnsPost() {
        UUID post1 = UUID.randomUUID();
        PostContentCacheDTO dto = samplePost(post1, "Single Post");
        when(cache.get(post1)).thenReturn(dto);

        Optional<PostContentCacheDTO> result = contentCache.getPost(post1);

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().content()).isEqualTo("Single Post");
        verifyNoInteractions(contentReader);
        assertThat(meterRegistry.counter("fakebook.cache.post_content.hits").count()).isEqualTo(1.0);
    }

    @Test
    void getPost_emptySentinel_returnsEmptyWithoutCallingReader() {
        UUID post1 = UUID.randomUUID();
        when(cache.get(post1)).thenReturn(PostContentCacheDTO.EMPTY);

        Optional<PostContentCacheDTO> result = contentCache.getPost(post1);

        assertThat(result).isEmpty();
        verifyNoInteractions(contentReader);
        assertThat(meterRegistry.counter("fakebook.cache.post_content.hits").count()).isEqualTo(1.0);
    }

    @Test
    void getPost_miss_loadsFromReaderAndCaches() {
        UUID post1 = UUID.randomUUID();
        when(cache.get(post1)).thenReturn(null);

        PostContentCacheDTO dto = samplePost(post1, "Loaded from DB");
        when(contentReader.loadFromDatabase(Set.of(post1))).thenReturn(Map.of(post1, dto));

        Optional<PostContentCacheDTO> result = contentCache.getPost(post1);

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().content()).isEqualTo("Loaded from DB");
        verify(cache).putAll(anyMap());
        assertThat(meterRegistry.counter("fakebook.cache.post_content.misses").count()).isEqualTo(1.0);
    }

    @Test
    void evict_removesKeyFromCache() {
        UUID post1 = UUID.randomUUID();

        contentCache.evict(post1);

        verify(cache).remove(post1);
    }

    @Test
    void evict_whenCacheThrows_doesNotPropagateException() {
        UUID post1 = UUID.randomUUID();
        doThrow(new RuntimeException("Redis connection error")).when(cache).remove(post1);

        contentCache.evict(post1);

        assertThat(meterRegistry.counter("fakebook.cache.post_content.errors", "operation", "evict").count()).isEqualTo(1.0);
    }

    @Test
    void evictAll_removesKeysFromCache() {
        UUID post1 = UUID.randomUUID();
        UUID post2 = UUID.randomUUID();

        contentCache.evictAll(List.of(post1, post2));

        verify(cache).removeAll(Set.of(post1, post2));
    }
}
