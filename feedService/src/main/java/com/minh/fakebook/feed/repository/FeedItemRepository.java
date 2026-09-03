package com.minh.fakebook.feed.repository;

import com.minh.fakebook.feed.domain.FeedItem;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the FeedItem entity.
 */
@SuppressWarnings("unused")
@Repository
public interface FeedItemRepository extends JpaRepository<FeedItem, UUID>, JpaSpecificationExecutor<FeedItem> {}
