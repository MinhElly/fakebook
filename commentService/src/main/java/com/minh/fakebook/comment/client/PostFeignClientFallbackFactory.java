package com.minh.fakebook.comment.client;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class PostFeignClientFallbackFactory implements FallbackFactory<PostFeignClient> {

    private static final Logger LOG = LoggerFactory.getLogger(PostFeignClientFallbackFactory.class);

    @Override
    public PostFeignClient create(Throwable cause) {
        return postId -> {
            RuntimeException failure = DownstreamFailureMapper.map("Post Service", "loading post " + postId, cause);
            if (failure instanceof org.springframework.web.server.ResponseStatusException) {
                LOG.debug("Post Service rejected request while loading post {}", postId);
            } else if (DownstreamFailureMapper.isCircuitOpen(cause)) {
                LOG.warn("Post Service Circuit Breaker is OPEN while loading post {}", postId);
            } else {
                LOG.error("Post Service unavailable while loading post {}", postId, cause);
            }
            throw failure;
        };
    }
}
