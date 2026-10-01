package com.minh.fakebook.post.repository;

import com.minh.fakebook.post.domain.SavedPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for the SavedPost entity.
 */
@SuppressWarnings("unused")
@Repository
public interface SavedPostRepository extends JpaRepository<SavedPost, UUID> {
    
    /**
     * Finds a saved post by user ID and post ID.
     * @param userId the user ID.
     * @param postId the post ID.
     * @return the saved post wrapped in Optional.
     */
    Optional<SavedPost> findByUserIdAndPostId(UUID userId, UUID postId);

    /**
     * Finds all saved posts for a user, ordered by creation time descending.
     * @param userId the user ID.
     * @return the list of saved posts.
     */
    List<SavedPost> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
