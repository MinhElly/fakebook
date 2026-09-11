package com.minh.fakebook.user.repository;

import com.minh.fakebook.user.domain.FriendRequest;
import com.minh.fakebook.user.domain.enumeration.FriendRequestStatus;

import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    java.util.Optional<FriendRequest> findBySenderIdAndReceiverIdAndStatus(UUID senderId, UUID receiverId,
            FriendRequestStatus status);

    Page<FriendRequest> findByReceiverIdAndStatus(UUID receiverId, FriendRequestStatus status, Pageable pageable);
    Page<FriendRequest> findBySenderIdAndStatus(UUID senderId, FriendRequestStatus status, Pageable pageable);

    @Query ("SELECT fr.receiver.id FROM FriendRequest fr WHERE fr.sender.id = :currentUserId AND fr.receiver.id IN :targetIds AND fr.status = :status")
    Set<UUID> findPendingReceiverIdsIn(@Param ("currentUserId") UUID currentUserId, @Param ("targetIds") Collection<UUID> targetIds, @Param ("status") FriendRequestStatus status);
    
    @Query ("SELECT fr.sender.id FROM FriendRequest fr WHERE fr.receiver.id = :currentUserId AND fr.sender.id IN :targetIds AND fr.status = :status")
    Set<UUID> findPendingSenderIdsIn(@Param ("currentUserId") UUID currentUserId, @Param ("targetIds") Collection<UUID> targetIds, @Param ("status") FriendRequestStatus status);
}
