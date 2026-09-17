package com.minh.fakebook.comment.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.UUID;

@FeignClient(name = "postservice", configuration = TokenRelayRequestInterceptor.class)
public interface PostFeignClient {

    @GetMapping("/api/posts/{id}")
    PostSyncDTO getPostById(@PathVariable("id") UUID id);

    record PostSyncDTO(UUID id, UUID authorId, String status, String visibility) {
    }
}