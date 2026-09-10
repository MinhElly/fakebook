package com.minh.fakebook.user.repository;

import com.minh.fakebook.user.domain.Friendship;

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
    
}