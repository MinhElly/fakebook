package com.minh.fakebook.user.client;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class MediaServiceClientFallbackFactory implements FallbackFactory<MediaServiceClient> {

    private static final Logger LOG = LoggerFactory.getLogger(MediaServiceClientFallbackFactory.class);

    @Override
    public MediaServiceClient create(Throwable cause) {
        return mediaId -> {
            RuntimeException failure = DownstreamFailureMapper.map("Media Service", "validating profile media " + mediaId, cause);
            if (failure instanceof org.springframework.web.server.ResponseStatusException) {
                LOG.debug("Media Service rejected profile media validation for {}", mediaId);
            } else if (DownstreamFailureMapper.isCircuitOpen(cause)) {
                LOG.warn("Media Service Circuit Breaker is OPEN while validating profile media {}", mediaId);
            } else {
                LOG.error("Media Service unavailable while validating profile media {}", mediaId, cause);
            }
            throw failure;
        };
    }
}
