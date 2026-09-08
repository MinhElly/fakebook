package com.minh.fakebook.user.repository;

import com.minh.fakebook.user.domain.Friendship;
import java.util.UUID;
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
}