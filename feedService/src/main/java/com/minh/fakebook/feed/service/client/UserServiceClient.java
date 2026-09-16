package com.minh.fakebook.feed.service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "userService", path = "/api/friendships")
public interface UserServiceClient {
    @GetMapping("/user/{user-id}")
    List<UUID> getUserFriendsList(@PathVariable("userId") UUID userId, @SpringQueryMap Pageable pageable);
}
