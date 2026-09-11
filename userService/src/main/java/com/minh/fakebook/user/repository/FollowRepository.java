package com.minh.fakebook.user.repository;

import com.minh.fakebook.user.domain.Follow;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Follow entity.
 */
@SuppressWarnings("unused")
@Repository
public interface FollowRepository extends JpaRepository<Follow, UUID>, JpaSpecificationExecutor<Follow> {
    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM Follow f WHERE f.follower.id = :followerId AND f.following.id = :followingId")
    boolean existsFollowing(@Param("followerId") UUID followerId, @Param("followingId") UUID followingId);

    @Modifying
    @Query("DELETE FROM Follow f WHERE f.follower.id = :followerId AND f.following.id = :followingId")
    void deleteFollowing(@Param("followerId") UUID followerId, @Param("followingId") UUID followingId);

    @Query("""
            SELECT f
            FROM Follow f
            WHERE f.follower.id = :userId
            """)
    Page<Follow> findFollowing(
            @Param("userId") UUID userId,
            Pageable pageable);

    @Query("""
                SELECT f
                FROM Follow f
                WHERE f.following.id = :userId
            """)
    Page<Follow> findFollowers(
            @Param("userId") UUID userId,
            Pageable pageable);
}
