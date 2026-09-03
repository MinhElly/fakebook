package com.minh.fakebook.user.repository;

import com.minh.fakebook.user.domain.Follow;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Follow entity.
 */
@SuppressWarnings("unused")
@Repository
public interface FollowRepository extends JpaRepository<Follow, UUID>, JpaSpecificationExecutor<Follow> {}
