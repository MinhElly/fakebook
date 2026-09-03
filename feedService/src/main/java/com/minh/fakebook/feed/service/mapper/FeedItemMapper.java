package com.minh.fakebook.feed.service.mapper;

import com.minh.fakebook.feed.domain.FeedItem;
import com.minh.fakebook.feed.service.dto.FeedItemDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link FeedItem} and its DTO {@link FeedItemDTO}.
 */
@Mapper(componentModel = "spring")
public interface FeedItemMapper extends EntityMapper<FeedItemDTO, FeedItem> {}
