package com.minh.fakebook.feed.client;
import java.util.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
@FeignClient(name = "userservice", contextId = "socialContextClient", configuration = TokenRelayRequestInterceptor.class)
public interface SocialContextClient {
    record SocialContext(Set<UUID> friends, Set<UUID> following) {}
    @GetMapping("/api/internal/social-context/{userId}") SocialContext read(@PathVariable UUID userId);
}
