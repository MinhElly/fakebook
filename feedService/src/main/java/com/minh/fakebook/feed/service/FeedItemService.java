package com.minh.fakebook.feed.service;

import com.minh.fakebook.feed.domain.FeedItem;
import com.minh.fakebook.feed.repository.FeedItemRepository;
import com.minh.fakebook.feed.service.dto.FeedItemDTO;
import com.minh.fakebook.feed.service.mapper.FeedItemMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.minh.fakebook.feed.domain.FeedItem}.
 */
@Service
@Transactional
public class FeedItemService {

    private static final Logger LOG = LoggerFactory.getLogger(FeedItemService.class);

    private final FeedItemRepository feedItemRepository;

    private final FeedItemMapper feedItemMapper;

    public FeedItemService(FeedItemRepository feedItemRepository, FeedItemMapper feedItemMapper) {
        this.feedItemRepository = feedItemRepository;
        this.feedItemMapper = feedItemMapper;
    }

    /**
     * Save a feedItem.
     *
     * @param feedItemDTO the entity to save.
     * @return the persisted entity.
     */
    public FeedItemDTO save(FeedItemDTO feedItemDTO) {
        LOG.debug("Request to save FeedItem : {}", feedItemDTO);
        FeedItem feedItem = feedItemMapper.toEntity(feedItemDTO);
        feedItem = feedItemRepository.save(feedItem);
        return feedItemMapper.toDto(feedItem);
    }

    /**
     * Update a feedItem.
     *
     * @param feedItemDTO the entity to save.
     * @return the persisted entity.
     */
    public FeedItemDTO update(FeedItemDTO feedItemDTO) {
        LOG.debug("Request to update FeedItem : {}", feedItemDTO);
        FeedItem feedItem = feedItemMapper.toEntity(feedItemDTO);
        feedItem = feedItemRepository.save(feedItem);
        return feedItemMapper.toDto(feedItem);
    }

    /**
     * Partially update a feedItem.
     *
     * @param feedItemDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<FeedItemDTO> partialUpdate(FeedItemDTO feedItemDTO) {
        LOG.debug("Request to partially update FeedItem : {}", feedItemDTO);

        return feedItemRepository
            .findById(feedItemDTO.getId())
            .map(existingFeedItem -> {
                feedItemMapper.partialUpdate(existingFeedItem, feedItemDTO);

                return existingFeedItem;
            })
            .map(feedItemRepository::save)
            .map(feedItemMapper::toDto);
    }

    /**
     * Get one feedItem by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<FeedItemDTO> findOne(UUID id) {
        LOG.debug("Request to get FeedItem : {}", id);
        return feedItemRepository.findById(id).map(feedItemMapper::toDto);
    }

    /**
     * Delete the feedItem by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete FeedItem : {}", id);
        feedItemRepository.deleteById(id);
    }
}
