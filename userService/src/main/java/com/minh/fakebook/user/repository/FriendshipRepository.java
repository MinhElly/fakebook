package com.minh.fakebook.user.repository;

import com.minh.fakebook.user.domain.Friendship;
import com.minh.fakebook.user.service.dto.FriendSuggestionProjection;

import jakarta.persistence.Tuple;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Friendship entity.
 */
@SuppressWarnings("unused")
@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, UUID>, JpaSpecificationExecutor<Friendship> {

    @Query("SELECT COUNT(f1.friend.id) FROM Friendship f1 JOIN Friendship f2 ON f1.friend.id = f2.friend.id WHERE f1.user.id = :userId1 AND f2.user.id = :userId2")
    long countMutualFriends(@Param("userId1") UUID userId1, @Param("userId2") UUID userId2);

    boolean existsByUserIdAndFriendId(UUID userId, UUID friendId);

    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM Friendship f WHERE (f.user.id = :userId1 AND f.friend.id = :userId2) OR (f.user.id = :userId2 AND f.friend.id = :userId1)")
    boolean existsFriendship(@Param("userId1") UUID userId1, @Param("userId2") UUID userId2);

    @Modifying
    @Query("DELETE FROM Friendship f WHERE (f.user.id = :userId1 AND f.friend.id = :userId2) OR (f.user.id = :userId2 AND f.friend.id = :userId1)")
    void deleteFriendship(@Param("userId1") UUID userId1, @Param("userId2") UUID userId2);

    Page<Friendship> findByUserId(UUID userId, Pageable pageable);

    @Query("SELECT f.friend.id FROM Friendship f WHERE f.user.id = :currentUserId AND f.friend.id IN :targetIds")
    Set<UUID> findFriendIdsIn(@Param ("currentUserId")UUID currentUserId, @Param ("targetIds") Collection<UUID> targetIds);

    @Query("SELECT f2.user.id AS userId, COUNT(f1.friend.id) AS count " + 
        "FROM Friendship f1 JOIN Friendship f2 ON f1.friend.id = f2.friend.id " +
        "WHERE f1.user.id = :currentUserId AND f2.user.id IN :targetIds " +
        "GROUP BY f2.user.id"
    )
    List<Tuple> countMutualFriendsForUsers(@Param("currentUserId") UUID currentUserId, @Param("targetIds") Collection<UUID> targetIds);

    @Query(value = """
    SELECT 
        up.id AS "userId",
        COUNT(DISTINCT mf.friend_id) AS "mutualFriendsCount"
    FROM user_profiles up
    LEFT JOIN friendships cf
        ON cf.user_id = up.id
    LEFT JOIN friendships mf
        ON mf.user_id = :currentUserId 
        AND mf.friend_id = cf.friend_id
    WHERE up.id <> :currentUserId
    AND NOT EXISTS (
        SELECT 1
        FROM friendships f
        WHERE f.user_id = :currentUserId
        AND f.friend_id = up.id
    )
    AND NOT EXISTS (
        SELECT 1
        FROM friend_requests fr
        WHERE fr.status = 'PENDING'
            AND (
                (
                    fr.sender_id = :currentUserId
                    AND fr.receiver_id = up.id
                )
                OR (
                    fr.sender_id = up.id
                    AND fr.receiver_id = :currentUserId 
                )
            )
    )
    GROUP BY up.id
    ORDER BY "mutualFriendsCount" DESC, up.id ASC
    """, nativeQuery = true)
    List<FriendSuggestionProjection> findFriendSuggestions(@Param("currentUserId") UUID currentUserId, Pageable pageable);
    
}