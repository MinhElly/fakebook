package com.minh.fakebook.post.client;

import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.minh.fakebook.post.config.FeignUserRelayRequestInterceptor;

@FeignClient(name = "userService", configuration = FeignUserRelayRequestInterceptor.class)
public interface UserClient {
    /**
     * Call userService to check if two users are friends.
     */
    @GetMapping("/api/friendships/check")
    boolean areFriends(@RequestParam("userId1") UUID userId1, @RequestParam("userId2") UUID
        userId2);
}
