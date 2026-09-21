package com.minh.fakebook.comment.client;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PostFeignClientFallback implements PostFeignClient {

    private static final Logger log = LoggerFactory.getLogger(PostFeignClientFallback.class);

    @Override
    public PostSyncDTO getPostById(UUID id) {
        log.warn("Fallback: Cannot fetch post details for ID {}", id);
        return null;
    }
}
