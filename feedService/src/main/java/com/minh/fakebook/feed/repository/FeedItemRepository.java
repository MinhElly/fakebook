package com.minh.fakebook.feed.repository;

import com.minh.fakebook.feed.domain.FeedItem;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the FeedItem entity.
 */
@SuppressWarnings("unused")
@Repository
public interface FeedItemRepository extends JpaRepository<FeedItem, UUID>, JpaSpecificationExecutor<FeedItem> {
    void deleteByPostId(UUID postId);

    Page<FeedItem> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
    List<FeedItem> findByUserIdAndPostIdIn(UUID userId, List<UUID> postId);
}
