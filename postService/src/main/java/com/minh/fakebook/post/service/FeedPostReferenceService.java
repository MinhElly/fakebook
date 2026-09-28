package com.minh.fakebook.post.service;

import com.minh.fakebook.post.domain.enumeration.PostStatus;
import com.minh.fakebook.post.domain.enumeration.PostVisibility;
import com.minh.fakebook.post.repository.PostRepository;
import com.minh.fakebook.post.service.dto.FeedPostReferenceDTO;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class FeedPostReferenceService {

    private final PostRepository postRepository;

    public FeedPostReferenceService(PostRepository postRepository) {
        this.postRepository = postRepository;
    }
    public List<FeedPostReferenceDTO> findRecentFriendsPosts(UUID authorId, int limit) {
        return postRepository
            .findByAuthorIdAndStatusAndVisibilityOrderByCreatedAtDescIdDesc(
                authorId,
                PostStatus.ACTIVE,
                PostVisibility.FRIENDS,
                PageRequest.of(0, limit)
            )
            .stream()
            .map(post -> new FeedPostReferenceDTO(post.getId(), post.getAuthorId(), post.getVisibility(), post.getCreatedAt()))
            .toList();
    }
}
