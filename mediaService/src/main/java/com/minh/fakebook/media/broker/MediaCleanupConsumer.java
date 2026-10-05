package com.minh.fakebook.media.broker;

import com.minh.fakebook.media.service.MediaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;
import java.util.function.Consumer;


@Component("processMediaCleanup") 
 public class MediaCleanupConsumer implements Consumer<String> {

    private static final Logger LOG = LoggerFactory.getLogger(MediaCleanupConsumer.class);
    private final ObjectMapper objectMapper;
    private final MediaService mediaService;

    public MediaCleanupConsumer(ObjectMapper objectMapper, MediaService mediaService) {
        this.objectMapper = objectMapper;
        this.mediaService = mediaService;
    }

    @Override
        public void accept(String payload) {
            try {
                JsonNode node = objectMapper.readTree(payload);
                if (node.has("mediaId") && !node.get("mediaId").isNull()) {
                    UUID mediaId = UUID.fromString(node.get("mediaId").asString());
                    LOG.info("Received media cleanup event for mediaId: {}",mediaId);
                    mediaService.deleteBySystem(mediaId);
                }
            } catch (Exception e) {
                LOG.error("Failed to process media cleanup event", e);
                throw new RuntimeException("Failed to process media cleanup event, triggering retry/DLT", e);
            }
        }
}