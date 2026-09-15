package com.minh.fakebook.comment.repository;

import com.minh.fakebook.comment.domain.PostCache;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the PostCache entity.
 */
@SuppressWarnings("unused")
@Repository
public interface PostCacheRepository extends JpaRepository<PostCache, UUID> {}
