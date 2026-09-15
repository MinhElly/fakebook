package com.minh.fakebook.media.broker;

import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.minh.fakebook.media.service.MediaService;
import com.minh.fakebook.media.service.dto.events.MediaCleanupEvent;

@Configuration 
public class MediaCleanupConsumer {
    private static final Logger LOG = LoggerFactory.getLogger(MediaCleanupConsumer.class);
    private final MediaService mediaService;

    public MediaCleanupConsumer(MediaService mediaService) {
        this.mediaService = mediaService;
    }
    
    @Bean 
    public Consumer<MediaCleanupEvent> processMediaCleanup(){
        return event -> {
            LOG.info("Received MediaCleanupEvent for mediaId: {}, reason: {}", event.mediaId(), event.reason());
            try{
                mediaService.deleteBySystem(event.mediaId());
            }catch(Exception e){
                LOG.error("Failed to delete media ID: {}", event.mediaId(), e);
            }
        };
    }
}
