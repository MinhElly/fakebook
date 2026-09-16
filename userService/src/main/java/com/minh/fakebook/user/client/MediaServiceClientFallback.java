package com.minh.fakebook.user.client;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class MediaServiceClientFallback implements MediaServiceClient {

    private static final Logger LOG = LoggerFactory.getLogger(MediaServiceClientFallback.class);

    @Override
    public MediaDTO getMediaById(UUID mediaId) {
        LOG.error("Fallback triggered: mediaService is unavailable. Throwing SERVICE_UNAVAILABLE for mediaId: {}", mediaId);
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Media service is currently unavailable");
    }

}
