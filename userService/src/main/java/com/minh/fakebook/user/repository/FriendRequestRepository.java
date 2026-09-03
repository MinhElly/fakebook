package com.minh.fakebook.user.repository;

import com.minh.fakebook.user.domain.FriendRequest;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the FriendRequest entity.
 */
@SuppressWarnings("unused")
@Repository
public interface FriendRequestRepository extends JpaRepository<FriendRequest, UUID>, JpaSpecificationExecutor<FriendRequest> {}
