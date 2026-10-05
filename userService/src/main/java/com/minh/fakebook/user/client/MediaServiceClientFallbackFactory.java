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
            LOG.error("Media Service unavailable while validating profile media {}", mediaId, cause);
            throw new DownstreamServiceUnavailableException("Media Service", "validating profile media " + mediaId, cause);
        };
    }
}
