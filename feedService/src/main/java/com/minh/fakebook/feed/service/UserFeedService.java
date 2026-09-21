package com.minh.fakebook.feed.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.minh.fakebook.feed.repository.FeedItemRepository;
import com.minh.fakebook.feed.service.dto.FeedItemDTO;
import com.minh.fakebook.feed.service.mapper.FeedItemMapper;
import com.minh.fakebook.feed.domain.UserFeedItemDocument;
import com.minh.fakebook.feed.repository.UserFeedItemDocumentRepository;

@Service
@Transactional(readOnly = true)
public class UserFeedService {
    private static final Logger LOG = LoggerFactory.getLogger(UserFeedService.class);

    private final StringRedisTemplate redisTemplate;
    private final FeedItemRepository feedItemRepository;
    private final FeedItemMapper feedItemMapper;
    private final UserFeedItemDocumentRepository mongoRepository;

    public UserFeedService(StringRedisTemplate redisTemplate, FeedItemRepository feedItemRepository,
            FeedItemMapper feedItemMapper, UserFeedItemDocumentRepository mongoRepository) {
        this.redisTemplate = redisTemplate;
        this.feedItemRepository = feedItemRepository;
        this.feedItemMapper = feedItemMapper;
        this.mongoRepository = mongoRepository;
    }

    public Page<FeedItemDTO> getUserFeed(UUID userId, Pageable pageable) {
            String redisKey = "feed:user:" + userId.toString();

            try {
                long start = pageable.getOffset();
                long end = start + pageable.getPageSize() - 1;
                Set<String> postIds = redisTemplate.opsForZSet().reverseRange(redisKey,
  start, end);

                if (postIds != null && !postIds.isEmpty()) {
                    List<UUID> uuids = postIds.stream().map(UUID::fromString).toList();

                    List<FeedItemDTO> dtos = new ArrayList<>();
                    for (UserFeedItemDocument doc : mongoRepository.
  findByUserIdAndPostIdIn(userId, uuids)) {
                        FeedItemDTO dto = new FeedItemDTO();
                        dto.setUserId(doc.getUserId());
                        dto.setPostId(doc.getPostId());
                        dto.setCreatedAt(doc.getCreatedAt());
                        dtos.add(dto);
                    }

                    Long total = redisTemplate.opsForZSet().zCard(redisKey);
                    return new PageImpl<>(dtos, pageable, total != null ? total : dtos.
  size());
                }
            } catch (Exception e) {
                LOG.warn("Failed to read feed from redis for user {}: {}. Falling back to MongoDB", userId, e.getMessage());
            }

            Page<UserFeedItemDocument> docs = mongoRepository.
  findByUserIdOrderByCreatedAtDesc(userId, pageable);
            List<FeedItemDTO> fallbackDtos = new ArrayList<>();
            for (UserFeedItemDocument doc : docs) {
                FeedItemDTO dto = new FeedItemDTO();
                dto.setUserId(doc.getUserId());
                dto.setPostId(doc.getPostId());
                dto.setCreatedAt(doc.getCreatedAt());
                fallbackDtos.add(dto);
            }
            return new PageImpl<>(fallbackDtos, pageable, docs.getTotalElements());
        }
}