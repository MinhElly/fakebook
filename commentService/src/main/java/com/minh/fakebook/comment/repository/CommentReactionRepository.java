package com.minh.fakebook.comment.repository;

import com.minh.fakebook.comment.domain.CommentReaction;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;
import java.util.Optional;

/**
 * Spring Data JPA repository for the CommentReaction entity.
 */
@SuppressWarnings("unused")
@Repository
public interface CommentReactionRepository
        extends JpaRepository<CommentReaction, UUID>, JpaSpecificationExecutor<CommentReaction> {
    Optional<CommentReaction> findByCommentIdAndUserId(UUID commentId, UUID userId);
}
