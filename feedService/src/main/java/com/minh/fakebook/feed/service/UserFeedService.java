package com.minh.fakebook.feed.service;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.minh.fakebook.feed.repository.FeedItemRepository;
import com.minh.fakebook.feed.service.dto.FeedItemDTO;
import com.minh.fakebook.feed.service.mapper.FeedItemMapper;
import com.minh.fakebook.feed.domain.FeedItem;

@Service
@Transactional(readOnly = true)
public class UserFeedService {
    private static final Logger LOG = LoggerFactory.getLogger(UserFeedService.class);

    private static final int CACHE_LIMIT = 500;

    private final StringRedisTemplate redisTemplate;
    private final FeedItemRepository feedItemRepository;
    private final FeedItemMapper feedItemMapper;

    public UserFeedService(StringRedisTemplate redisTemplate, FeedItemRepository feedItemRepository, FeedItemMapper feedItemMapper) {
        this.redisTemplate = redisTemplate;
        this.feedItemRepository = feedItemRepository;
        this.feedItemMapper = feedItemMapper;
    }

    public Page<FeedItemDTO> getUserFeed(UUID userId, Pageable pageable) {
        String redisKey = feedKey(userId);
        boolean redisMiss = false;

        try {
            long start = pageable.getOffset();
            long end = start + pageable.getPageSize() - 1;
            Set<String> cachedPostIds = redisTemplate.opsForZSet().reverseRange(redisKey, start, end);

            if (cachedPostIds != null && !cachedPostIds.isEmpty()) {
                List<String> orderedPostIdStrings = List.copyOf(cachedPostIds);
                List<UUID> orderedPostIds = orderedPostIdStrings.stream().map(UUID::fromString).toList();
                List<FeedItem> feedItems = feedItemRepository.findByUserIdAndPostIdIn(userId, orderedPostIds);
                Map<UUID, FeedItem> itemsByPostId = new HashMap<>();
                for (FeedItem feedItem : feedItems) {
                    itemsByPostId.put(feedItem.getPostId(), feedItem);
                }

                List<FeedItemDTO> orderedDtos = orderedPostIds
                    .stream()
                    .map(itemsByPostId::get)
                    .filter(java.util.Objects::nonNull)
                    .map(feedItemMapper::toDto)
                    .toList();

                for (int index = 0; index < orderedPostIds.size(); index++) {
                    if (!itemsByPostId.containsKey(orderedPostIds.get(index))) {
                        redisTemplate.opsForZSet().remove(redisKey, orderedPostIdStrings.get(index));
                    }
                }

                Long total = redisTemplate.opsForZSet().zCard(redisKey);
                if (!orderedDtos.isEmpty() || (total != null && total > 0)) {
                    return new PageImpl<>(orderedDtos, pageable, total != null ? total : orderedDtos.size());
                }
                redisMiss = true;
            } else {
                Long cachedItemCount = redisTemplate.opsForZSet().zCard(redisKey);
                redisMiss = cachedItemCount == null || cachedItemCount == 0;
            }
        } catch (Exception e) {
            LOG.warn("Failed to read feed from Redis for user {}: {}. Falling back to MariaDB", userId, e.getMessage());
        }

        Page<FeedItem> page = feedItemRepository.findByUserIdOrderByCreatedAtDescPostIdDesc(userId, pageable);
        if (redisMiss && pageable.getOffset() < CACHE_LIMIT) {
            warmUpUserFeedCache(userId);
        }
        return page.map(feedItemMapper::toDto);
    }

    private void warmUpUserFeedCache(UUID userId) {
        try {
            Page<FeedItem> cachePage = feedItemRepository.findByUserIdOrderByCreatedAtDescPostIdDesc(
                userId,
                PageRequest.of(0, CACHE_LIMIT)
            );
            if (cachePage.isEmpty()) {
                return;
            }

            Set<ZSetOperations.TypedTuple<String>> tuples = new LinkedHashSet<>();
            for (FeedItem item : cachePage.getContent()) {
                tuples.add(ZSetOperations.TypedTuple.of(item.getPostId().toString(), (double) item.getCreatedAt().toEpochMilli()));
            }

            String redisKey = feedKey(userId);
            redisTemplate.opsForZSet().add(redisKey, tuples);
            redisTemplate.opsForZSet().removeRange(redisKey, 0, -(CACHE_LIMIT + 1L));
            LOG.debug("Warmed feed cache for user {} with {} items", userId, tuples.size());
        } catch (Exception e) {
            LOG.warn("Failed to warm feed cache for user {}: {}", userId, e.getMessage());
        }
    }

    private String feedKey(UUID userId) {
        return "feed:user:" + userId;
    }
}
