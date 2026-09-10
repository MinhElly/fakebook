package com.minh.fakebook.user.repository;

import com.minh.fakebook.user.domain.UserProfile;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the UserProfile entity.
 */
@SuppressWarnings("unused")
@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, UUID>, JpaSpecificationExecutor<UserProfile> {
    Page<UserProfile> findByDisplayNameContainingIgnoreCaseOrUsernameContainingIgnoreCase(String displayName, String username, Pageable pageable);
}

