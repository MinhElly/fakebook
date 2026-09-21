package com.minh.fakebook.post.client;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component 
public class MediaServiceClientFallback implements MediaServiceClient {
    
    private static final Logger log = LoggerFactory.getLogger(MediaServiceClientFallback.class);

    @Override
    public MediaValidationDTO getMedia(UUID id) {
        log.warn("Fallback: Cannot fetch media details for ID {}", id);
        return null;
    }
}
