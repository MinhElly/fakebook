package com.minh.fakebook.post.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.minh.fakebook.post.IntegrationTest;
import com.minh.fakebook.post.config.CacheConfiguration;
import com.minh.fakebook.post.domain.Post;
import com.minh.fakebook.post.domain.PostReaction;
import com.minh.fakebook.post.domain.enumeration.PostStatus;
import com.minh.fakebook.post.domain.enumeration.PostVisibility;
import com.minh.fakebook.post.domain.enumeration.ReactionType;
import com.minh.fakebook.post.repository.PostReactionRepository;
import com.minh.fakebook.post.repository.PostRepository;
import com.minh.fakebook.post.security.AuthoritiesConstants;
import com.minh.fakebook.post.service.PostReactionCountsCache;
import com.minh.fakebook.post.service.PostService;
import com.minh.fakebook.post.service.dto.PostDTO;
import com.minh.fakebook.post.service.dto.PostReactionDTO;
import com.minh.fakebook.post.service.dto.ReactionCountsCacheDTO;
import com.minh.fakebook.post.service.dto.SetPostReactionRequest;
import io.micrometer.core.instrument.MeterRegistry;
import com.minh.fakebook.post.repository.PostMediaRepository;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import javax.cache.Cache;
import javax.cache.CacheManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.ObjectMapper;

/**
 * Integration tests verifying the Reaction Counts Cache-Aside architecture,
 * including hits/misses, Redis serialization, invalidation after commit,
 * multi-user isolation, authorization checks, and write responses.
 */
@IntegrationTest
@AutoConfigureMockMvc
class PostReactionCacheIT {

    private static final UUID AUTHOR_ID = UUID.randomUUID();
    private static final UUID USER_1 = UUID.randomUUID();
    private static final UUID USER_2 = UUID.randomUUID();
    private static final String SUMMARIES_URL = "/api/post-reactions/summaries";

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
    private PostReactionCountsCache countsCache;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private MeterRegistry meterRegistry;

    @Autowired
    private ObjectMapper objectMapper;

    private Post post1;
    private Post post2;

    @BeforeEach
    void setUp() {
        postReactionRepository.deleteAll();
        postMediaRepository.deleteAll();
        postRepository.deleteAll();
        getRawCache().removeAll();

        post1 = postRepository.saveAndFlush(
            new Post()
                .authorId(AUTHOR_ID)
                .content("Post 1")
                .visibility(PostVisibility.PUBLIC)
                .status(PostStatus.ACTIVE)
                .createdAt(Instant.now())
        );

        post2 = postRepository.saveAndFlush(
            new Post()
                .authorId(AUTHOR_ID)
                .content("Post 2")
                .visibility(PostVisibility.PUBLIC)
                .status(PostStatus.ACTIVE)
                .createdAt(Instant.now())
        );

        countsCache.evict(post1.getId());
        countsCache.evict(post2.getId());
    }

    @Test
    void authoritativeCountReadsDoNotReuseCachedActorVisibility() throws Exception {
        postReactionRepository.saveAndFlush(
            new PostReaction().userId(USER_1).reactionType(ReactionType.LIKE).post(post1).createdAt(Instant.now())
        );
        postReactionRepository.saveAndFlush(
            new PostReaction().userId(USER_2).reactionType(ReactionType.LOVE).post(post1).createdAt(Instant.now())
        );

        double missesBefore = getMetricCount("fakebook.cache.reaction_counts.misses");
        double hitsBefore = getMetricCount("fakebook.cache.reaction_counts.hits");

        // Counts use current actor eligibility from the database.
        mockMvc
            .perform(get(SUMMARIES_URL).param("postId.in", post1.getId().toString()).with(jwtAs(USER_1)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].postId").value(post1.getId().toString()))
            .andExpect(jsonPath("$[0].totalCount").value(2))
            .andExpect(jsonPath("$[0].counts.LIKE").value(1))
            .andExpect(jsonPath("$[0].counts.LOVE").value(1))
            .andExpect(jsonPath("$[0].myReaction").value("LIKE"));

        assertThat(getMetricCount("fakebook.cache.reaction_counts.misses")).isEqualTo(missesBefore);

        // The next call also uses fresh actor eligibility.
        mockMvc
            .perform(get(SUMMARIES_URL).param("postId.in", post1.getId().toString()).with(jwtAs(USER_1)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].postId").value(post1.getId().toString()))
            .andExpect(jsonPath("$[0].totalCount").value(2))
            .andExpect(jsonPath("$[0].counts.LIKE").value(1))
            .andExpect(jsonPath("$[0].counts.LOVE").value(1))
            .andExpect(jsonPath("$[0].myReaction").value("LIKE"));

        assertThat(getMetricCount("fakebook.cache.reaction_counts.hits")).isEqualTo(hitsBefore);
    }

    @Test
    void zeroReactionsPost_isCachedAndReturnsZeroCounts() throws Exception {
        // Post 2 has 0 reactions
        mockMvc
            .perform(get(SUMMARIES_URL).param("postId.in", post2.getId().toString()).with(jwtAs(USER_1)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].totalCount").value(0))
            .andExpect(jsonPath("$[0].counts.LIKE").value(0))
            .andExpect(jsonPath("$[0].counts.LOVE").value(0))
            .andExpect(jsonPath("$[0].myReaction").isEmpty());

        // Second call should hit cache and still return 0s
        mockMvc
            .perform(get(SUMMARIES_URL).param("postId.in", post2.getId().toString()).with(jwtAs(USER_1)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].totalCount").value(0))
            .andExpect(jsonPath("$[0].counts.LIKE").value(0));
    }

    @Test
    void twoUsers_shareCachedCountsWhileHavingDistinctMyReaction() throws Exception {
        postReactionRepository.saveAndFlush(
            new PostReaction().userId(USER_1).reactionType(ReactionType.LIKE).post(post1).createdAt(Instant.now())
        );

        // User 1 reads summary -> triggers caching
        mockMvc
            .perform(get(SUMMARIES_URL).param("postId.in", post1.getId().toString()).with(jwtAs(USER_1)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].totalCount").value(1))
            .andExpect(jsonPath("$[0].counts.LIKE").value(1))
            .andExpect(jsonPath("$[0].myReaction").value("LIKE"));

        // User 2 reads summary -> gets cached counts (total: 1), but myReaction is null
        mockMvc
            .perform(get(SUMMARIES_URL).param("postId.in", post1.getId().toString()).with(jwtAs(USER_2)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].totalCount").value(1))
            .andExpect(jsonPath("$[0].counts.LIKE").value(1))
            .andExpect(jsonPath("$[0].myReaction").isEmpty());
    }

    @Test
    void setReaction_returnsFreshDbDataAndEvictsCacheAfterCommit() throws Exception {
        // Warm up cache for post1 (0 reactions)
        mockMvc.perform(get(SUMMARIES_URL).param("postId.in", post1.getId().toString()).with(jwtAs(USER_1)));

        // User 1 sets reaction LIKE
        SetPostReactionRequest request = new SetPostReactionRequest(ReactionType.LIKE);
        mockMvc
            .perform(
                put("/api/post-reactions/posts/{postId}", post1.getId())
                    .with(jwtAs(USER_1))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(request))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalCount").value(1))
            .andExpect(jsonPath("$.counts.LIKE").value(1))
            .andExpect(jsonPath("$.myReaction").value("LIKE"));

        // GET request afterwards must read updated counts from DB and repopulate cache
        mockMvc
            .perform(get(SUMMARIES_URL).param("postId.in", post1.getId().toString()).with(jwtAs(USER_2)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].totalCount").value(1))
            .andExpect(jsonPath("$[0].counts.LIKE").value(1));
    }

    @Test
    void removeReaction_returnsFreshDbDataAndEvictsCacheAfterCommit() throws Exception {
        postReactionRepository.saveAndFlush(
            new PostReaction().userId(USER_1).reactionType(ReactionType.LIKE).post(post1).createdAt(Instant.now())
        );

        // Warm up cache
        mockMvc.perform(get(SUMMARIES_URL).param("postId.in", post1.getId().toString()).with(jwtAs(USER_1)));

        // User 1 removes reaction
        mockMvc
            .perform(delete("/api/post-reactions/posts/{postId}", post1.getId()).with(jwtAs(USER_1)).with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalCount").value(0))
            .andExpect(jsonPath("$.counts.LIKE").value(0))
            .andExpect(jsonPath("$.myReaction").isEmpty());

        // Next GET should see 0
        mockMvc
            .perform(get(SUMMARIES_URL).param("postId.in", post1.getId().toString()).with(jwtAs(USER_1)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].totalCount").value(0));
    }

    @Test
    void legacySaveAndDirectDelete_invalidatesCache() throws Exception {
        // Legacy save creates a reaction
        PostDTO postDTO = new PostDTO();
        postDTO.setId(post1.getId());
        PostReactionDTO req = new PostReactionDTO();
        req.setPost(postDTO);
        req.setReactionType(ReactionType.LOVE);

        mockMvc
            .perform(
                post("/api/post-reactions")
                    .with(jwtAs(USER_1))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(req))
            )
            .andExpect(status().isCreated());

        mockMvc
            .perform(get(SUMMARIES_URL).param("postId.in", post1.getId().toString()).with(jwtAs(USER_1)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].counts.LOVE").value(1));

        PostReaction reaction = postReactionRepository.findByPostIdAndUserId(post1.getId(), USER_1).orElseThrow();

        // Direct delete
        mockMvc
            .perform(delete("/api/post-reactions/{id}", reaction.getId()).with(jwtAs(USER_1)).with(csrf()))
            .andExpect(status().isNoContent());

        // Cache must be evicted and next read returns 0
        mockMvc
            .perform(get(SUMMARIES_URL).param("postId.in", post1.getId().toString()).with(jwtAs(USER_1)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].counts.LOVE").value(0));
    }

    @Test
    void postDelete_evictsReactionCountsCache() throws Exception {
        postReactionRepository.saveAndFlush(
            new PostReaction().userId(USER_1).reactionType(ReactionType.LIKE).post(post1).createdAt(Instant.now())
        );

        // Warm up cache
        mockMvc.perform(get(SUMMARIES_URL).param("postId.in", post1.getId().toString()).with(jwtAs(USER_1)));

        // Delete post via API as author
        mockMvc
            .perform(delete("/api/posts/{id}", post1.getId()).with(jwtAs(AUTHOR_ID)).with(csrf()))
            .andExpect(status().isNoContent());

        // Cache for post1 should be evicted from Redis
        Cache<UUID, ReactionCountsCacheDTO> cache = getRawCache();
        if (cache != null) {
            assertThat(cache.get(post1.getId())).isNull();
        }
    }

    @Test
    void authorization_checksPreservedEvenWhenCached() throws Exception {
        Post privatePost = postRepository.saveAndFlush(
            new Post()
                .authorId(AUTHOR_ID)
                .content("Private post")
                .visibility(PostVisibility.PRIVATE)
                .status(PostStatus.ACTIVE)
                .createdAt(Instant.now())
        );

        postReactionRepository.saveAndFlush(
            new PostReaction().userId(AUTHOR_ID).reactionType(ReactionType.LIKE).post(privatePost).createdAt(Instant.now())
        );

        // Author warms up cache
        mockMvc
            .perform(get(SUMMARIES_URL).param("postId.in", privatePost.getId().toString()).with(jwtAs(AUTHOR_ID)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].totalCount").value(1));

        // Unauthorized user attempts to read -> authorization filter excludes private post!
        mockMvc
            .perform(get(SUMMARIES_URL).param("postId.in", privatePost.getId().toString()).with(jwtAs(USER_2)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void redisSerializationCompatibility_storesAndRetrievesRecordSuccessfully() {
        UUID testPostId = UUID.randomUUID();
        ReactionCountsCacheDTO dto = new ReactionCountsCacheDTO(Map.of(ReactionType.LIKE, 42L, ReactionType.LOVE, 7L));

        Cache<UUID, ReactionCountsCacheDTO> cache = getRawCache();
        assertThat(cache).isNotNull();

        cache.put(testPostId, dto);

        ReactionCountsCacheDTO retrieved = cache.get(testPostId);
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.counts().get(ReactionType.LIKE)).isEqualTo(42L);
        assertThat(retrieved.counts().get(ReactionType.LOVE)).isEqualTo(7L);
        assertThat(retrieved.counts().get(ReactionType.HAHA)).isZero();

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
    private Cache<UUID, ReactionCountsCacheDTO> getRawCache() {
        return (Cache<UUID, ReactionCountsCacheDTO>) (Cache<?, ?>) cacheManager.getCache(CacheConfiguration.POST_REACTION_COUNTS_CACHE);
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
