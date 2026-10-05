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
            LOG.error("Post Service unavailable while loading post {}", postId, cause);
            throw new DownstreamServiceUnavailableException("Post Service", "loading post " + postId, cause);
        };
    }
}
