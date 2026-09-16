package com.minh.fakebook.feed.service.client;

import com.minh.fakebook.feed.client.TokenRelayRequestInterceptor;
import java.util.List;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "userService", path = "/api/friendships", configuration = TokenRelayRequestInterceptor.class)
public interface UserServiceClient {

    @GetMapping("/user/{userId}/friend-ids")
    List<UUID> getUserFriendsList(@PathVariable("userId") UUID userId);
}
