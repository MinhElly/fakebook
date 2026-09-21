package com.minh.fakebook.feed.repository;

import com.minh.fakebook.feed.domain.UserFeedItemDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserFeedItemDocumentRepository extends
        MongoRepository<UserFeedItemDocument, String> {
    Page<UserFeedItemDocument> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    List<UserFeedItemDocument> findByUserIdAndPostIdIn(UUID userId, List<UUID> postIds);

    void deleteByPostId(UUID postId);
}