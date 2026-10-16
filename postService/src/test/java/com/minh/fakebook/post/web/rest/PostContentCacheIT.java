package com.minh.fakebook.post.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.minh.fakebook.post.IntegrationTest;
import com.minh.fakebook.post.client.UserServiceClient;
import com.minh.fakebook.post.config.CacheConfiguration;
import com.minh.fakebook.post.domain.Post;
import com.minh.fakebook.post.domain.PostMedia;
import com.minh.fakebook.post.domain.enumeration.PostStatus;
import com.minh.fakebook.post.domain.enumeration.PostVisibility;
import com.minh.fakebook.post.repository.PostMediaRepository;
import com.minh.fakebook.post.repository.PostRepository;
import com.minh.fakebook.post.security.AuthoritiesConstants;
import com.minh.fakebook.post.service.PostContentCache;
import com.minh.fakebook.post.service.PostService;
import com.minh.fakebook.post.service.dto.PostContentCacheDTO;
import com.minh.fakebook.post.service.dto.PostDTO;
import io.micrometer.core.instrument.MeterRegistry;
import com.minh.fakebook.post.repository.PostReactionRepository;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.cache.Cache;
import javax.cache.CacheManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.ObjectMapper;

/**
 * Integration tests verifying the Post Content Cache-Aside architecture,
 * including single & batch hits/misses, negative caching, dynamic authorization,
 * after-commit invalidation, and Redis serialization compatibility.
 */
@IntegrationTest
@AutoConfigureMockMvc
class PostContentCacheIT {

    private static final UUID AUTHOR_ID = UUID.randomUUID();
    private static final UUID USER_1 = UUID.randomUUID();
    private static final UUID USER_2 = UUID.randomUUID();
    private static final String POSTS_URL = "/api/posts";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostMediaRepository postMediaRepository;

    @Autowired
    private PostReactionRepository postReactionRepository;

    @Autowired
    private PostService postService;

    @Autowired
    private PostContentCache postContentCache;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private MeterRegistry meterRegistry;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserServiceClient userServiceClient;

    private Post post1;
    private Post post2;

    @BeforeEach
    void setUp() {
        Mockito.when(userServiceClient.getFriendIdsByUserId(Mockito.any())).thenReturn(List.of());
        Mockito.when(userServiceClient.areFriends(Mockito.any(), Mockito.any())).thenReturn(false);

        postReactionRepository.deleteAll();
        postMediaRepository.deleteAll();
        postRepository.deleteAll();
        getRawCache().removeAll();

        post1 = postRepository.saveAndFlush(
            new Post()
                .authorId(AUTHOR_ID)
                .content("Post Content 1")
                .visibility(PostVisibility.PUBLIC)
                .status(PostStatus.ACTIVE)
                .createdAt(Instant.now())
        );

        post2 = postRepository.saveAndFlush(
            new Post()
                .authorId(AUTHOR_ID)
                .content("Post Content 2")
                .visibility(PostVisibility.PUBLIC)
                .status(PostStatus.ACTIVE)
                .createdAt(Instant.now())
        );

        postContentCache.evict(post1.getId());
        postContentCache.evict(post2.getId());
    }

    @Test
    void singlePost_cacheMissThenHitFlow_recordsMetricsAndPopulatesCache() throws Exception {
        UUID mediaId = UUID.randomUUID();
        PostMedia pm = new PostMedia();
        pm.setPost(post1);
        pm.setMediaId(mediaId);
        pm.setDisplayOrder(0);
        pm.setCreatedAt(Instant.now());
        postMediaRepository.saveAndFlush(pm);

        double missesBefore = getMetricCount("fakebook.cache.post_content.misses");
        double hitsBefore = getMetricCount("fakebook.cache.post_content.hits");

        // 1st request: Misses cache -> loads from MariaDB -> populates Redis cache
        mockMvc
            .perform(get(POSTS_URL + "/{id}", post1.getId()).with(jwtAs(AUTHOR_ID)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(post1.getId().toString()))
            .andExpect(jsonPath("$.content").value("Post Content 1"))
            .andExpect(jsonPath("$.mediaIds[0]").value(mediaId.toString()));

        assertThat(getMetricCount("fakebook.cache.post_content.misses")).isEqualTo(missesBefore + 1);

        // Verify Redis cache contains entry
        Cache<UUID, PostContentCacheDTO> rawCache = getRawCache();
        assertThat(rawCache.get(post1.getId())).isNotNull();
        assertThat(rawCache.get(post1.getId()).content()).isEqualTo("Post Content 1");

        // 2nd request: Hits Redis cache!
        mockMvc
            .perform(get(POSTS_URL + "/{id}", post1.getId()).with(jwtAs(AUTHOR_ID)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(post1.getId().toString()))
            .andExpect(jsonPath("$.content").value("Post Content 1"));

        assertThat(getMetricCount("fakebook.cache.post_content.hits")).isEqualTo(hitsBefore + 1);
    }

    @Test
    void batchFeedHydration_fastPath_servesMultiplePostsFromCache() throws Exception {
        double missesBefore = getMetricCount("fakebook.cache.post_content.misses");
        double hitsBefore = getMetricCount("fakebook.cache.post_content.hits");

        // First batch read: both miss and populate cache
        mockMvc
            .perform(get(POSTS_URL + "?id.in=" + post1.getId() + "," + post2.getId() + "&size=50").with(jwtAs(AUTHOR_ID)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2));

        assertThat(getMetricCount("fakebook.cache.post_content.misses")).isEqualTo(missesBefore + 2);

        // Second batch read: both hit cache!
        mockMvc
            .perform(get(POSTS_URL + "?id.in=" + post1.getId() + "," + post2.getId() + "&size=50").with(jwtAs(AUTHOR_ID)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2));

        assertThat(getMetricCount("fakebook.cache.post_content.hits")).isEqualTo(hitsBefore + 2);
    }

    @Test
    void dynamicAuthorization_privatePost_isolatedPerCaller() throws Exception {
        Post privatePost = postRepository.saveAndFlush(
            new Post()
                .authorId(AUTHOR_ID)
                .content("Private Secret")
                .visibility(PostVisibility.PRIVATE)
                .status(PostStatus.ACTIVE)
                .createdAt(Instant.now())
        );

        // Author views: 200 OK -> cached in Redis
        mockMvc
            .perform(get(POSTS_URL + "/{id}", privatePost.getId()).with(jwtAs(AUTHOR_ID)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").value("Private Secret"));

        // Verify it is cached
        assertThat(getRawCache().get(privatePost.getId())).isNotNull();

        // Other user attempts to view: dynamic authorization prevents access -> 403 Forbidden
        mockMvc
            .perform(get(POSTS_URL + "/{id}", privatePost.getId()).with(jwtAs(USER_2)))
            .andExpect(status().isForbidden());
    }

    @Test
    void dynamicAuthorization_friendsPost_authorizesFriendsOnly() throws Exception {
        Post friendsPost = postRepository.saveAndFlush(
            new Post()
                .authorId(AUTHOR_ID)
                .content("Friends Only Story")
                .visibility(PostVisibility.FRIENDS)
                .status(PostStatus.ACTIVE)
                .createdAt(Instant.now())
        );

        Mockito.when(userServiceClient.areFriends(USER_1, AUTHOR_ID)).thenReturn(true);
        Mockito.when(userServiceClient.areFriends(USER_2, AUTHOR_ID)).thenReturn(false);

        // Friend views: 200 OK -> cached in Redis
        mockMvc
            .perform(get(POSTS_URL + "/{id}", friendsPost.getId()).with(jwtAs(USER_1)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").value("Friends Only Story"));

        // Non-friend attempts to view: 403 Forbidden even though post is cached
        mockMvc
            .perform(get(POSTS_URL + "/{id}", friendsPost.getId()).with(jwtAs(USER_2)))
            .andExpect(status().isForbidden());
    }

    @Test
    void invalidation_onPostUpdate_evictsCache() throws Exception {
        // Cache post1 first
        mockMvc
            .perform(get(POSTS_URL + "/{id}", post1.getId()).with(jwtAs(AUTHOR_ID)))
            .andExpect(status().isOk());
        assertThat(getRawCache().get(post1.getId())).isNotNull();

        // Update post1
        PostDTO updateRequest = new PostDTO();
        updateRequest.setId(post1.getId());
        updateRequest.setAuthorId(AUTHOR_ID);
        updateRequest.setContent("Updated Content");
        updateRequest.setVisibility(PostVisibility.PUBLIC);
        updateRequest.setStatus(PostStatus.ACTIVE);
        updateRequest.setVersion(post1.getVersion());

        mockMvc
            .perform(
                put(POSTS_URL + "/{id}", post1.getId())
                    .with(jwtAs(AUTHOR_ID))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(updateRequest))
            )
            .andExpect(status().isOk());

        // Cache entry must be evicted
        assertThat(getRawCache().get(post1.getId())).isNull();

        // Next read returns updated content
        mockMvc
            .perform(get(POSTS_URL + "/{id}", post1.getId()).with(jwtAs(AUTHOR_ID)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").value("Updated Content"));
    }

    @Test
    void invalidation_onPostDelete_evictsCacheAndReturnsNotFound() throws Exception {
        // Cache post1
        mockMvc
            .perform(get(POSTS_URL + "/{id}", post1.getId()).with(jwtAs(AUTHOR_ID)))
            .andExpect(status().isOk());
        assertThat(getRawCache().get(post1.getId())).isNotNull();

        // Delete post1
        mockMvc
            .perform(delete(POSTS_URL + "/{id}", post1.getId()).with(jwtAs(AUTHOR_ID)).with(csrf()))
            .andExpect(status().isNoContent());

        // Next read must return 404
        mockMvc
            .perform(get(POSTS_URL + "/{id}", post1.getId()).with(jwtAs(AUTHOR_ID)))
            .andExpect(status().isNotFound());
    }

    @Test
    void negativeCaching_nonExistentPost_cachesEmptySentinel() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        // 1st request -> 404 Not Found
        mockMvc
            .perform(get(POSTS_URL + "/{id}", nonExistentId).with(jwtAs(AUTHOR_ID)))
            .andExpect(status().isNotFound());

        // Verify EMPTY sentinel in Redis cache
        PostContentCacheDTO cached = getRawCache().get(nonExistentId);
        assertThat(cached).isNotNull();
        assertThat(cached.isEmpty()).isTrue();

        double hitsBefore = getMetricCount("fakebook.cache.post_content.hits");

        // 2nd request -> hits EMPTY sentinel, returns 404 without querying DB
        mockMvc
            .perform(get(POSTS_URL + "/{id}", nonExistentId).with(jwtAs(AUTHOR_ID)))
            .andExpect(status().isNotFound());

        assertThat(getMetricCount("fakebook.cache.post_content.hits")).isEqualTo(hitsBefore + 1);
    }

    @Test
    void redisSerializationCompatibility_storesAndRetrievesRecordSuccessfully() {
        UUID testPostId = UUID.randomUUID();
        UUID mediaId1 = UUID.randomUUID();
        UUID tagId1 = UUID.randomUUID();

        PostContentCacheDTO dto = new PostContentCacheDTO(
            testPostId,
            AUTHOR_ID,
            "Complex content with emoji \uD83D\uDE80",
            PostVisibility.PUBLIC,
            PostStatus.ACTIVE,
            Instant.now(),
            Instant.now(),
            List.of(mediaId1),
            Set.of(tagId1),
            2L
        );

        Cache<UUID, PostContentCacheDTO> cache = getRawCache();
        assertThat(cache).isNotNull();

        cache.put(testPostId, dto);

        PostContentCacheDTO retrieved = cache.get(testPostId);
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.id()).isEqualTo(testPostId);
        assertThat(retrieved.content()).isEqualTo("Complex content with emoji \uD83D\uDE80");
        assertThat(retrieved.mediaIds()).containsExactly(mediaId1);
        assertThat(retrieved.taggedUserIds()).containsExactly(tagId1);
        assertThat(retrieved.version()).isEqualTo(2L);

        cache.remove(testPostId);
    }

    @AfterEach
    void tearDown() {
        postReactionRepository.deleteAll();
        postMediaRepository.deleteAll();
        postRepository.deleteAll();
        getRawCache().removeAll();
    }

    @SuppressWarnings("unchecked")
    private Cache<UUID, PostContentCacheDTO> getRawCache() {
        return (Cache<UUID, PostContentCacheDTO>) (Cache<?, ?>) cacheManager.getCache(CacheConfiguration.POST_CONTENT_CACHE);
    }

    private double getMetricCount(String name) {
        var counter = meterRegistry.find(name).counter();
        return counter != null ? counter.count() : 0.0;
    }

    private static RequestPostProcessor jwtAs(UUID userId) {
        return jwt()
            .jwt(token -> token.subject(userId.toString()))
            .authorities(new SimpleGrantedAuthority(AuthoritiesConstants.USER));
    }
}
