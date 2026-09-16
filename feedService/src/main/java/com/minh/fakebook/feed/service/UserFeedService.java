package com.minh.fakebook.feed.service;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.minh.fakebook.feed.repository.FeedItemRepository;
import com.minh.fakebook.feed.service.dto.FeedItemDTO;
import com.minh.fakebook.feed.service.mapper.FeedItemMapper;

public class UserFeedService {
    private static final Logger LOG = LoggerFactory.getLogger(UserFeedService.class);
    
    private final StringRedisTemplate redisTemplate;
    private final FeedItemRepository feedItemRepository;
    private final FeedItemMapper feedItemMapper;

    public UserFeedService(StringRedisTemplate redisTemplate, FeedItemRepository feedItemRepository, FeedItemMapper feedItemMapper) {
        this.redisTemplate = redisTemplate;
        this.feedItemRepository = feedItemRepository;
        this.feedItemMapper = feedItemMapper;
    }

    public Page<FeedItemDTO> getUserFeed(UUID userId, Pageable pageable){
        String redisKey = "feed:user" + userId.toString();
        try{
            long start = pageable.getOffset();
            long end = start + pageable.getPageSize() - 1;
            Set<String> postIds = redisTemplate.opsForZSet().reverseRange(redisKey, start, end);
            if(postIds != null && !postIds.isEmpty()){
                List<UUID> uuids = postIds.stream().map(UUID::fromString).toList();
                List<FeedItemDTO> dtos = feedItemRepository.findByUserIdAndPostIdIn(userId, uuids).stream().map(feedItemMapper::toDto).toList();
                Long total = redisTemplate.opsForZSet().zCard(redisKey);
                return new PageImpl<>(dtos, pageable, total != null ? total : dtos.size());
            }
        } catch (Exception e) {
            LOG.warn("Failled to read feed from redis for user {}: {}. Falling back to DB", userId, e.getMessage());
        }
        return feedItemRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable).map(feedItemMapper::toDto);
    } 
}
