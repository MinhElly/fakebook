package com.minh.fakebook.feed.repository;

import com.minh.fakebook.feed.domain.FeedItem;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring Data JPA repository for the FeedItem entity.
 */
@SuppressWarnings("unused")
@Repository
public interface FeedItemRepository extends JpaRepository<FeedItem, UUID>, JpaSpecificationExecutor<FeedItem> {
    @Modifying
    @Query(
        value = "INSERT IGNORE INTO feed_items (id, user_id, post_id, author_id, visibility, created_at) " +
        "VALUES (:id, :userId, :postId, :authorId, :visibility, :createdAt)",
        nativeQuery = true
    )
    int insertIgnore(
        @Param("id") String id,
        @Param("userId") String userId,
        @Param("postId") String postId,
        @Param("authorId") String authorId,
        @Param("visibility") String visibility,
        @Param("createdAt") java.time.Instant createdAt
    );

    void deleteByPostId(UUID postId);
    List<FeedItem> findByPostId(UUID postId);

    Page<FeedItem> findByUserIdOrderByCreatedAtDescPostIdDesc(UUID userId, Pageable pageable);
    List<FeedItem> findByUserIdAndPostIdIn(UUID userId, List<UUID> postIds);
    List<FeedItem> findByUserIdAndAuthorIdAndVisibility(UUID userId, UUID authorId, String visibility);

    @Transactional
    void deleteByUserIdAndAuthorIdAndVisibility(UUID userId, UUID authorId, String visibility);
}
