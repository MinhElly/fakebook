package com.minh.fakebook.post.repository;

import com.minh.fakebook.post.domain.PostMedia;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the PostMedia entity.
 */
@SuppressWarnings("unused")
@Repository
public interface PostMediaRepository extends JpaRepository<PostMedia, UUID>, JpaSpecificationExecutor<PostMedia> {
    java.util.List<PostMedia> findByPostIdOrderByDisplayOrderAsc(java.util.UUID postId);
}
