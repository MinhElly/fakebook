package com.minh.fakebook.comment.client;
import com.minh.fakebook.comment.client.dto.PostSyncDTO;

import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "postservice", configuration = TokenRelayRequestInterceptor.class, fallbackFactory = PostFeignClientFallbackFactory.class)
public interface PostFeignClient {

    @GetMapping("/api/posts/{id}")
    PostSyncDTO getPostById(@PathVariable("id") UUID id);

}
