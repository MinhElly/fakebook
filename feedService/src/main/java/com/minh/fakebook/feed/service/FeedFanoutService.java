package com.minh.fakebook.feed.service;

import com.minh.fakebook.feed.service.client.UserServiceClient;
import com.minh.fakebook.feed.service.dto.FeedItemDTO;
import com.minh.fakebook.feed.service.dto.event.PostCreatedEvent;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class FeedFanoutService {
    private final Logger LOG =  LoggerFactory.getLogger(FeedFanoutService.class);
    private final UserServiceClient userServiceClient;
    private final StringRedisTemplate redisTemplate;
    private final FeedItemService feedItemService;

    public FeedFanoutService(UserServiceClient userServiceClient,StringRedisTemplate redisTemplate, FeedItemService feedItemService) {
        this.userServiceClient = userServiceClient;
        this.redisTemplate = redisTemplate;
        this.feedItemService = feedItemService;
    }
    @Async
    @Transactional
    public void processPostCreated(PostCreatedEvent event){
        LOG.debug("Processing fan-out for postId: {}, visibility: {}", event.getPostId(), event.getVisibility());
        List<UUID> targetUserId = new ArrayList<>();
        if("PRIVATE".equals(event.getVisibility())){
            targetUserId.add(event.getAuthorId());
        }else{
            try{
                Pageable unpaged = Pageable.unpaged();
                var friendships = userServiceClient.getUserFriendsList(event.getAuthorId(), unpaged);
                if(friendships != null && !friendships.isEmpty()){
                    targetUserId.addAll(friendships);
                }
            }catch (Exception e){
                LOG.error("Failed to fetch friends list for user {}", event.getAuthorId(), e);
            }
            if(!targetUserId.contains(event.getAuthorId())){
                targetUserId.add(event.getAuthorId());
            }
        }
        if(targetUserId.isEmpty()){
            return;
        }
        double score = event.getCreatedAt().toEpochMilli();
        String postIdStr = event.getPostId().toString();
        int maxFeedSize = 500;
        for(UUID recipientId: targetUserId){
            String redisKey = "feed:user" + recipientId.toString();
            redisTemplate.opsForZSet().add(redisKey, postIdStr, score);
            redisTemplate.opsForZSet().removeRange(redisKey, 0, -(maxFeedSize+1));
        }
        for(UUID recipentId: targetUserId){
            FeedItemDTO dto = new FeedItemDTO();
            dto.setUserId(recipentId);
            dto.setPostId(event.getPostId());
            dto.setCreatedAt(event.getCreatedAt());
            feedItemService.save(dto);
        }
        LOG.debug("Fan-out for postId: {} completed. Processed {} recipients", event.getPostId(), targetUserId.size());
    }
}
