package com.minh.fakebook.feed.client;

import java.util.List;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "userservice", configuration = TokenRelayRequestInterceptor.class, fallback = UserServiceFallback.class)
public interface UserServiceClient {

    @GetMapping("/api/friendships/user/{userId}/friend-ids")
    List<UUID> getUserFriendsList(@PathVariable("userId") UUID userId);

     @GetMapping("/api/follows/user/{userId}/follower-ids")
    List<UUID> getUserFollowersList(@PathVariable("userId") UUID userId);
    
}
