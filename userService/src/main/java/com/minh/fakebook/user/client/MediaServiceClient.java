package com.minh.fakebook.user.client;
import com.minh.fakebook.user.client.dto.MediaDTO;

import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "mediaservice", path = "/api/media", fallbackFactory = MediaServiceClientFallbackFactory.class)
public interface MediaServiceClient {

    @GetMapping("/{id}")
    MediaDTO getMediaById(@PathVariable("id") UUID id);

}
