package com.minh.fakebook.post.client;
import com.minh.fakebook.post.client.dto.MediaValidationDTO;

import com.minh.fakebook.post.config.FeignUserRelayRequestInterceptor;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Feign client to communicate with mediaService for media validation.
 */
@FeignClient(
    name = "mediaservice",
    configuration = FeignUserRelayRequestInterceptor.class,
    fallbackFactory = MediaServiceClientFallbackFactory.class
)
public interface MediaServiceClient {

    @GetMapping("/api/media/{id}")
    MediaValidationDTO getMedia(@PathVariable("id") UUID id);
}
