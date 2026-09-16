package com.minh.fakebook.user.client;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MediaServiceClientFallback implements MediaServiceClient {

    private static final Logger LOG = LoggerFactory.getLogger(MediaServiceClientFallback.class);

    @Override
    public MediaDTO getMediaById(UUID mediaId) {
        LOG.warn("Fallback triggered: mediaService is unavailable. Returning default media fallback for mediaId: {}", mediaId);
        return new MediaDTO(mediaId, null, "/content/images/default-avatar.png", "FALLBACK");
    }

}
