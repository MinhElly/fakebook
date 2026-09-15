package com.minh.fakebook.user.service.client;

import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
@FeignClient (name = "mediaService", path = "/api/media")
public interface MediaClient {
    @GetMapping ("/{id}")
    MediaDTO getMediaById(@PathVariable ("id") UUID id);

    record MediaDTO(
        UUID id,
        UUID ownerId,
        String url,
        String status
    ){}
}
