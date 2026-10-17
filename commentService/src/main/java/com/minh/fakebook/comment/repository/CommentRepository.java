package com.minh.fakebook.comment.repository;

import com.minh.fakebook.comment.domain.Comment;
import com.minh.fakebook.comment.domain.enumeration.CommentStatus;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Comment entity.
 */
@SuppressWarnings("unused")
@Repository
public interface CommentRepository extends JpaRepository<Comment, UUID>, JpaSpecificationExecutor<Comment> {

    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Comment c where c.id = :id")
    java.util.Optional<Comment> findByIdForUpdate(@Param("id") UUID id);

    interface CommentCountProjection {
        UUID getPostId();

        long getCommentCount();
    }

    @Query(
        """
        select c.postId as postId, count(c.id) as commentCount
        from Comment c
        where c.postId in :postIds and c.status = :status
        group by c.postId
        """
    )
    List<CommentCountProjection> countByPostIdsAndStatus(
        @Param("postIds") Collection<UUID> postIds,
        @Param("status") CommentStatus status
    );

    @Query("""
        select c from Comment c where c.postId in :postIds and c.status = 'ACTIVE' and c.parentComment is null
        and not exists (select newer.id from Comment newer where newer.postId=c.postId
          and newer.status='ACTIVE' and newer.parentComment is null
          and (newer.createdAt>c.createdAt or (newer.createdAt=c.createdAt and newer.id>c.id)))
        """)
    List<Comment> findLatestActiveRootComments(@Param("postIds") Collection<UUID> postIds);
}
