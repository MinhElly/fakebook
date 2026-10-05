package com.minh.fakebook.post.repository;

import com.minh.fakebook.post.domain.Post;
import com.minh.fakebook.post.domain.enumeration.PostStatus;
import com.minh.fakebook.post.domain.enumeration.PostVisibility;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Post entity.
 */
@SuppressWarnings("unused")
@Repository
public interface PostRepository extends JpaRepository<Post, UUID>, JpaSpecificationExecutor<Post> {
    List<Post> findByAuthorIdAndStatusAndVisibilityOrderByCreatedAtDescIdDesc(
        UUID authorId,
        PostStatus status,
        PostVisibility visibility,
        Pageable pageable
    );
}
