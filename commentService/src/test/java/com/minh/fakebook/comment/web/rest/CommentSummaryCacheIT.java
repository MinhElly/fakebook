package com.minh.fakebook.comment.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.minh.fakebook.comment.IntegrationTest;
import com.minh.fakebook.comment.config.Constants;
import com.minh.fakebook.comment.domain.Comment;
import com.minh.fakebook.comment.domain.PostCache;
import com.minh.fakebook.comment.domain.enumeration.CommentStatus;
import com.minh.fakebook.comment.repository.CommentRepository;
import com.minh.fakebook.comment.repository.PostCacheRepository;
import com.minh.fakebook.comment.service.CommentService;
import com.minh.fakebook.comment.service.CommentSummaryCache;
import com.minh.fakebook.comment.service.dto.CommentSummaryCacheDTO;
import com.minh.fakebook.comment.service.dto.CommentSummaryRequest;
import com.minh.fakebook.comment.service.dto.CreateCommentRequestDTO;
import com.minh.fakebook.comment.service.dto.ReplyCommentRequestDTO;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import javax.cache.Cache;
import javax.cache.CacheManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

@IntegrationTest
@AutoConfigureMockMvc
class CommentSummaryCacheIT {

    private static final String SUMMARIES_URL = "/api/comments/summaries";
    private static final UUID AUTHOR_ID = UUID.randomUUID();
    private static final UUID OTHER_USER_ID = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private PostCacheRepository postCacheRepository;

    @Autowired
    private CommentService commentService;

    @Autowired
    private CommentSummaryCache commentSummaryCache;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private MeterRegistry meterRegistry;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private com.minh.fakebook.comment.client.PostFeignClient authoritativePosts;

    private PostCache publicPost;

    @BeforeEach
    void setUp() {
        commentRepository.deleteAll();
        postCacheRepository.deleteAll();

        Cache<UUID, CommentSummaryCacheDTO> cache = getCache();
        if (cache != null) {
            cache.clear();
        }

        org.mockito.Mockito.when(authoritativePosts.getPostById(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
            UUID id = invocation.getArgument(0);
            return postCacheRepository.findById(id)
                .map(p -> new com.minh.fakebook.comment.client.dto.PostSyncDTO(p.getId(), p.getAuthorId(), p.getStatus(), p.getVisibility()))
                .orElseThrow(() -> new IllegalArgumentException("Post fixture not found"));
        });

        publicPost = postCacheRepository.saveAndFlush(
            new PostCache().authorId(AUTHOR_ID).visibility("PUBLIC").status("ACTIVE")
        );
    }

    @Test
    void shouldReadAuthoritativeSummariesWithoutCachingPermissionSensitiveCounts() throws Exception {
        saveComment(publicPost.getId(), "Initial comment", CommentStatus.ACTIVE, null, Instant.now());

        double initialHits = getHitCount();
        double initialMisses = getMissCount();

        CommentSummaryRequest request = new CommentSummaryRequest(List.of(publicPost.getId()));
        byte[] requestBody = objectMapper.writeValueAsBytes(request);

        // Community counts are read from the authoritative database, never a cached allow decision.
        mockMvc.perform(
            post(SUMMARIES_URL)
                .with(jwt().jwt(t -> t.subject(AUTHOR_ID.toString())))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].commentCount").value(1))
            .andExpect(jsonPath("$[0].previewComment.content").value("Initial comment"));

        assertThat(getMissCount()).isEqualTo(initialMisses);
        assertThat(getHitCount()).isEqualTo(initialHits);

        // The community endpoint must not cache permission-sensitive counts.
        Cache<UUID, CommentSummaryCacheDTO> cache = getCache();
        assertThat(cache).isNotNull();
        assertThat(cache.get(publicPost.getId())).isNull();

        // The next call must make the same fresh read.
        mockMvc.perform(
            post(SUMMARIES_URL)
                .with(jwt().jwt(t -> t.subject(AUTHOR_ID.toString())))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].commentCount").value(1))
            .andExpect(jsonPath("$[0].previewComment.content").value("Initial comment"));

        assertThat(getHitCount()).isEqualTo(initialHits);
    }

    @Test
    void shouldEvictCacheWhenNewCommentIsCreated() throws Exception {
        CommentSummaryRequest request = new CommentSummaryRequest(List.of(publicPost.getId()));
        byte[] requestBody = objectMapper.writeValueAsBytes(request);

        // Warm cache with 0 comments
        mockMvc.perform(
            post(SUMMARIES_URL)
                .with(jwt().jwt(t -> t.subject(AUTHOR_ID.toString())))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].commentCount").value(0));

        // Create comment via service inside a transaction
        transactionTemplate.executeWithoutResult(status -> {
            commentService.createComment(new CreateCommentRequestDTO(publicPost.getId(), "Freshly created"), AUTHOR_ID);
        });

        // Request again: cache must be evicted, returning count = 1
        mockMvc.perform(
            post(SUMMARIES_URL)
                .with(jwt().jwt(t -> t.subject(AUTHOR_ID.toString())))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].commentCount").value(1))
            .andExpect(jsonPath("$[0].previewComment.content").value("Freshly created"));
    }

    @Test
    void shouldEvictCacheWhenCommentIsUpdated() throws Exception {
        Comment rootComment = saveComment(publicPost.getId(), "Original content", CommentStatus.ACTIVE, null, Instant.now());

        CommentSummaryRequest request = new CommentSummaryRequest(List.of(publicPost.getId()));
        byte[] requestBody = objectMapper.writeValueAsBytes(request);

        // Warm cache
        mockMvc.perform(
            post(SUMMARIES_URL)
                .with(jwt().jwt(t -> t.subject(AUTHOR_ID.toString())))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].previewComment.content").value("Original content"));

        // Update comment
        transactionTemplate.executeWithoutResult(status -> {
            commentService.updateComment(rootComment.getId(), "Edited content", AUTHOR_ID);
        });

        // Verify cache updated
        mockMvc.perform(
            post(SUMMARIES_URL)
                .with(jwt().jwt(t -> t.subject(AUTHOR_ID.toString())))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].previewComment.content").value("Edited content"))
            .andExpect(jsonPath("$[0].previewComment.editCount").value(1))
            .andExpect(jsonPath("$[0].previewComment.editedAt").isNotEmpty());
    }

    @Test
    void shouldEvictCacheWhenCommentIsDeleted() throws Exception {
        Instant t1 = Instant.parse("2026-10-15T10:00:00Z");
        Instant t2 = Instant.parse("2026-10-15T11:00:00Z");
        Comment olderRoot = saveComment(publicPost.getId(), "Older root", CommentStatus.ACTIVE, null, t1);
        Comment newerRoot = saveComment(publicPost.getId(), "Newer root", CommentStatus.ACTIVE, null, t2);

        CommentSummaryRequest request = new CommentSummaryRequest(List.of(publicPost.getId()));
        byte[] requestBody = objectMapper.writeValueAsBytes(request);

        // Warm cache: count = 2, preview = "Newer root"
        mockMvc.perform(
            post(SUMMARIES_URL)
                .with(jwt().jwt(t -> t.subject(AUTHOR_ID.toString())))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].commentCount").value(2))
            .andExpect(jsonPath("$[0].previewComment.id").value(newerRoot.getId().toString()));

        // Delete newer root
        transactionTemplate.executeWithoutResult(status -> {
            commentService.deleteComment(newerRoot.getId(), AUTHOR_ID, false);
        });

        // Query again: count = 1, preview switches to "Older root"
        mockMvc.perform(
            post(SUMMARIES_URL)
                .with(jwt().jwt(t -> t.subject(AUTHOR_ID.toString())))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].commentCount").value(1))
            .andExpect(jsonPath("$[0].previewComment.id").value(olderRoot.getId().toString()))
            .andExpect(jsonPath("$[0].previewComment.content").value("Older root"));
    }

    @Test
    void shouldEvictCacheWhenReplyIsAdded() throws Exception {
        Comment rootComment = saveComment(publicPost.getId(), "Parent comment", CommentStatus.ACTIVE, null, Instant.now());

        CommentSummaryRequest request = new CommentSummaryRequest(List.of(publicPost.getId()));
        byte[] requestBody = objectMapper.writeValueAsBytes(request);

        // Warm cache: count = 1
        mockMvc.perform(
            post(SUMMARIES_URL)
                .with(jwt().jwt(t -> t.subject(AUTHOR_ID.toString())))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].commentCount").value(1));

        // Reply to comment
        transactionTemplate.executeWithoutResult(status -> {
            commentService.replyToComment(new ReplyCommentRequestDTO(rootComment.getId(), "A lovely reply"), OTHER_USER_ID);
        });

        // Query again: count = 2, preview remains parent comment
        mockMvc.perform(
            post(SUMMARIES_URL)
                .with(jwt().jwt(t -> t.subject(AUTHOR_ID.toString())))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].commentCount").value(2))
            .andExpect(jsonPath("$[0].previewComment.id").value(rootComment.getId().toString()));
    }

    @Test
    void shouldReturnFreshZeroCountWithoutCaching() throws Exception {
        PostCache emptyPost = postCacheRepository.saveAndFlush(
            new PostCache().authorId(AUTHOR_ID).visibility("PUBLIC").status("ACTIVE")
        );

        CommentSummaryRequest request = new CommentSummaryRequest(List.of(emptyPost.getId()));
        byte[] requestBody = objectMapper.writeValueAsBytes(request);

        // First call
        mockMvc.perform(
            post(SUMMARIES_URL)
                .with(jwt().jwt(t -> t.subject(AUTHOR_ID.toString())))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].commentCount").value(0))
            .andExpect(jsonPath("$[0].previewComment").doesNotExist());

        // The public endpoint does not reuse cached count authorization.
        Cache<UUID, CommentSummaryCacheDTO> cache = getCache();
        assertThat(cache.get(emptyPost.getId())).isNull();

        double hitsBefore = getHitCount();
        // Second call: served from Redis cache
        mockMvc.perform(
            post(SUMMARIES_URL)
                .with(jwt().jwt(t -> t.subject(AUTHOR_ID.toString())))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].commentCount").value(0))
            .andExpect(jsonPath("$[0].previewComment").doesNotExist());

        assertThat(getHitCount()).isEqualTo(hitsBefore);
    }

    @Test
    void shouldEnforceAuthorizationBeforeCacheLookup() throws Exception {
        PostCache privatePost = postCacheRepository.saveAndFlush(
            new PostCache().authorId(AUTHOR_ID).visibility("PRIVATE").status("ACTIVE")
        );

        CommentSummaryRequest request = new CommentSummaryRequest(List.of(privatePost.getId()));
        byte[] requestBody = objectMapper.writeValueAsBytes(request);

        // Other user attempts to fetch summary -> Forbidden
        mockMvc.perform(
            post(SUMMARIES_URL)
                .with(jwt().jwt(t -> t.subject(OTHER_USER_ID.toString())))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isForbidden());
    }

    private Comment saveComment(UUID postId, String content, CommentStatus status, Comment parent, Instant createdAt) {
        Comment comment = new Comment()
            .postId(postId)
            .authorId(AUTHOR_ID)
            .content(content)
            .status(status)
            .parentComment(parent);
        comment.setCreatedAt(createdAt);
        return commentRepository.saveAndFlush(comment);
    }

    @SuppressWarnings("unchecked")
    private Cache<UUID, CommentSummaryCacheDTO> getCache() {
        return (Cache<UUID, CommentSummaryCacheDTO>) (Cache<?, ?>) cacheManager.getCache(Constants.COMMENT_SUMMARY_CACHE);
    }

    private double getHitCount() {
        var counter = meterRegistry.find("fakebook.cache.comment_summary.hits").counter();
        return counter != null ? counter.count() : 0.0;
    }

    private double getMissCount() {
        var counter = meterRegistry.find("fakebook.cache.comment_summary.misses").counter();
        return counter != null ? counter.count() : 0.0;
    }
}
