package com.minh.fakebook.user.repository;

import com.minh.fakebook.user.domain.FriendRequest;
import com.minh.fakebook.user.domain.enumeration.FriendRequestStatus;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the FriendRequest entity.
 */
@SuppressWarnings("unused")
@Repository
public interface FriendRequestRepository
        extends JpaRepository<FriendRequest, UUID>, JpaSpecificationExecutor<FriendRequest> {
    boolean existsBySenderIdAndReceiverIdAndStatus(UUID senderId, UUID receiverId, FriendRequestStatus status);
    java.util.Optional<FriendRequest> findBySenderIdAndReceiverIdAndStatus(UUID senderId, UUID receiverId, FriendRequestStatus status);
}
