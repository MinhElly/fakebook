package com.minh.fakebook.comment.service;

import com.minh.fakebook.comment.domain.PostCache;
import com.minh.fakebook.comment.repository.PostCacheRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class PostCacheResolver {

    private final PostCacheRepository postCacheRepository;

    private final PostCacheWriter postCacheWriter;

    public PostCacheResolver(PostCacheRepository postCacheRepository, PostCacheWriter postCacheWriter) {
        this.postCacheRepository = postCacheRepository;
        this.postCacheWriter = postCacheWriter;
    }

    public PostCache resolve(UUID postId) {
        return postCacheRepository.findById(postId).orElseGet(() -> postCacheWriter.fetchAndCache(postId));
    }
}
