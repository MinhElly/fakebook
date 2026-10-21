package com.minh.fakebook.post.repository;

import com.minh.fakebook.post.domain.PostMedia;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the PostMedia entity.
 */
@SuppressWarnings("unused")
@Repository
public interface PostMediaRepository extends JpaRepository<PostMedia, UUID>, JpaSpecificationExecutor<PostMedia> {
    List<PostMedia> findByPostIdOrderByDisplayOrderAsc(UUID postId);

    List<PostMedia> findByPostIdInOrderByDisplayOrderAsc(Collection<UUID> postIds);

    List<PostMedia> findByMediaId(UUID mediaId);
    boolean existsByMediaId(UUID mediaId);

    void deleteByPostId(UUID postId);
}
