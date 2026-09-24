package com.minh.fakebook.comment.service;

import com.minh.fakebook.comment.client.PostFeignClient;
import com.minh.fakebook.comment.domain.PostCache;
import com.minh.fakebook.comment.repository.PostCacheRepository;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostCacheResolver {

    private final PostCacheRepository postCacheRepository;
    private final PostFeignClient postFeignClient;

    private final JdbcTemplate jdbcTemplate;

    public PostCacheResolver(
        PostCacheRepository postCacheRepository,
        PostFeignClient postFeignClient,
        JdbcTemplate jdbcTemplate
    ) {
        this.postCacheRepository = postCacheRepository;
        this.postFeignClient = postFeignClient;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PostCache resolve(UUID postId) {
        return postCacheRepository.findById(postId).orElseGet(() -> fetchAndCache(postId));
    }

    private PostCache fetchAndCache(UUID postId) {
        PostFeignClient.PostSyncDTO post = postFeignClient.getPostById(postId);
        if (post == null) {
            throw new IllegalArgumentException("Post not found: " + postId);
        }

        PostCache postCache = new PostCache();
        postCache.setId(post.id());
        postCache.setAuthorId(post.authorId());
        postCache.setStatus(post.status());
        postCache.setVisibility(post.visibility());
        jdbcTemplate.update(
            "INSERT INTO post_cache (id, author_id, status, visibility) VALUES (?, ?, ?, ?) " +
            "ON DUPLICATE KEY UPDATE author_id = VALUES(author_id), status = VALUES(status), visibility = VALUES(visibility)",
            post.id().toString(),
            post.authorId().toString(),
            post.status(),
            post.visibility()
        );
        return postCache;
    }
}
