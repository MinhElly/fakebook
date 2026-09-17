package com.minh.fakebook.comment.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

/**
 * Feign Client to communicate with userService.
 */
@FeignClient(name = "userservice", configuration = TokenRelayRequestInterceptor.class, fallback = UserServiceFallback.class)
public interface UserServiceClient {

    /**
     * Calls userService to check friendship status.
     *
     * @param userId1 the first user ID
     * @param userId2 the second user ID
     * @return true if they are friends, false otherwise
     */
    @GetMapping("/api/friendships/check")
    boolean checkFriendship(@RequestParam("userId1") UUID userId1, @RequestParam("userId2") UUID userId2);
}
