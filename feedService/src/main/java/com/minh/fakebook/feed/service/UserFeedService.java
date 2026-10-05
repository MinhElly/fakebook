package com.minh.fakebook.feed.service;

import com.minh.fakebook.feed.repository.FeedItemRepository;
import com.minh.fakebook.feed.service.dto.FeedItemDTO;
import com.minh.fakebook.feed.service.mapper.FeedItemMapper;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserFeedService {

    private final FeedItemRepository feedItemRepository;
    private final FeedItemMapper feedItemMapper;

    public UserFeedService(FeedItemRepository feedItemRepository, FeedItemMapper feedItemMapper) {
        this.feedItemRepository = feedItemRepository;
        this.feedItemMapper = feedItemMapper;
    }

    public Page<FeedItemDTO> getUserFeed(UUID userId, Pageable pageable) {
        return feedItemRepository.findByUserIdOrderByCreatedAtDescPostIdDesc(userId, pageable).map(feedItemMapper::toDto);
    }
}
