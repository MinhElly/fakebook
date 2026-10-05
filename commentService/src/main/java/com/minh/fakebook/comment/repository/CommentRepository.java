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

    @Query(
        value = """
        select ranked.id, ranked.post_id, ranked.author_id, ranked.content,
               ranked.status, ranked.parent_comment_id, ranked.created_at
        from (
            select c.*,
                   row_number() over (
                       partition by c.post_id
                       order by c.created_at desc, c.id desc
                   ) as rn
            from comments c
            where c.post_id in (:postIds)
              and c.status = 'ACTIVE'
              and c.parent_comment_id is null
        ) ranked
        where ranked.rn = 1
        """,
        nativeQuery = true
    )
    List<Comment> findLatestActiveRootComments(@Param("postIds") Collection<UUID> postIds);
}
