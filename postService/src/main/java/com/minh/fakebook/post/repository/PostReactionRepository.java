package com.minh.fakebook.post.repository;

import com.minh.fakebook.post.domain.PostReaction;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the PostReaction entity.
 */
@SuppressWarnings("unused")
@Repository
public interface PostReactionRepository
        extends JpaRepository<PostReaction, UUID>, JpaSpecificationExecutor<PostReaction> {
    Optional<PostReaction> findByPostIdAndUserId(UUID postId,
                    UUID userId);
            
@Modifying
@Query("DELETE FROM PostReaction pr WHERE pr.post.id = :postId")
void deleteByPostId(@Param("postId") UUID postId);
}
