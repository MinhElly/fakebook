package com.minh.fakebook.feed.client;

import java.util.List;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Feign Client to communicate with userService.
 */
@FeignClient(name = "userService", configuration = TokenRelayRequestInterceptor.class)
public interface UserFeignClient {

    /**
     * Fetch list of friend IDs for a given user ID.
     */
    @GetMapping("/api/friendships/user/{userId}/friend-ids")
    List<UUID> getFriendIdsByUserId(@PathVariable("userId") UUID userId);
}
