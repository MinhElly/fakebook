package com.minh.fakebook.post.repository;

import com.minh.fakebook.post.domain.PostReaction;
import com.minh.fakebook.post.domain.enumeration.ReactionType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PostReactionRepository extends JpaRepository<PostReaction, UUID>, JpaSpecificationExecutor<PostReaction> {
    interface ReactionCountProjection {
        UUID getPostId();

        ReactionType getReactionType();

        long getReactionCount();
    }

    Optional<PostReaction> findByPostIdAndUserId(UUID postId, UUID userId);

    List<PostReaction> findAllByPostIdInAndUserId(Collection<UUID> postIds, UUID userId);

    Page<PostReaction> findAllByPostId(UUID postId, Pageable pageable);

    @Query(
        """
        select
            pr.post.id as postId,
            pr.reactionType as reactionType,
            count(pr) as reactionCount
        from PostReaction pr
        where pr.post.id in :postIds
        group by pr.post.id, pr.reactionType
        """
    )
    List<ReactionCountProjection> countByPostIds(@Param("postIds") Collection<UUID> postIds);

    @Modifying
    @Query("delete from PostReaction pr where pr.post.id = :postId")
    void deleteByPostId(@Param("postId") UUID postId);
}
