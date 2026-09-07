package com.minh.fakebook.post.repository;

import com.minh.fakebook.post.domain.PostReaction;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the PostReaction entity.
 */
@SuppressWarnings("unused")
@Repository
public interface PostReactionRepository
        extends JpaRepository<PostReaction, UUID>, JpaSpecificationExecutor<PostReaction> {
    java.util.Optional<com.minh.fakebook.post.domain.PostReaction> findByPostIdAndUserId(java.util.UUID postId,
                    java.util.UUID userId);
            
@org.springframework.data.jpa.repository.Modifying
@org.springframework.data.jpa.repository.Query("DELETE FROM PostReaction pr WHERE pr.post.id = :postId")
void deleteByPostId(@org.springframework.data.repository.query.Param("postId") java.util.UUID postId);
}
