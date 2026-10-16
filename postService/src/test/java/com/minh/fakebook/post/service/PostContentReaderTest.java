package com.minh.fakebook.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.minh.fakebook.post.domain.Post;
import com.minh.fakebook.post.domain.PostMedia;
import com.minh.fakebook.post.domain.enumeration.PostStatus;
import com.minh.fakebook.post.domain.enumeration.PostVisibility;
import com.minh.fakebook.post.repository.PostMediaRepository;
import com.minh.fakebook.post.repository.PostRepository;
import com.minh.fakebook.post.service.dto.PostContentCacheDTO;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PostContentReaderTest {

    private PostRepository postRepository;
    private PostMediaRepository postMediaRepository;
    private PostContentReader contentReader;

    @BeforeEach
    void setUp() {
        postRepository = mock(PostRepository.class);
        postMediaRepository = mock(PostMediaRepository.class);
        contentReader = new PostContentReader(postRepository, postMediaRepository);
    }

    @Test
    void loadFromDatabase_withEmptyCollection_returnsEmptyMap() {
        assertThat(contentReader.loadFromDatabase(null)).isEmpty();
        assertThat(contentReader.loadFromDatabase(List.of())).isEmpty();
        verifyNoInteractions(postRepository);
        verifyNoInteractions(postMediaRepository);
    }

    @Test
    void loadFromDatabase_loadsPostsAndGroupsMediaInDisplayOrder() {
        UUID post1Id = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        Post post1 = new Post();
        post1.setId(post1Id);
        post1.setAuthorId(authorId);
        post1.setContent("Post with media");
        post1.setVisibility(PostVisibility.PUBLIC);
        post1.setStatus(PostStatus.ACTIVE);
        post1.setCreatedAt(Instant.now());

        UUID media1 = UUID.randomUUID();
        UUID media2 = UUID.randomUUID();

        PostMedia pm1 = new PostMedia();
        pm1.setId(UUID.randomUUID());
        pm1.setPost(post1);
        pm1.setMediaId(media1);
        pm1.setDisplayOrder(0);

        PostMedia pm2 = new PostMedia();
        pm2.setId(UUID.randomUUID());
        pm2.setPost(post1);
        pm2.setMediaId(media2);
        pm2.setDisplayOrder(1);

        when(postRepository.findAllById(List.of(post1Id))).thenReturn(List.of(post1));
        when(postMediaRepository.findByPostIdInOrderByDisplayOrderAsc(List.of(post1Id))).thenReturn(List.of(pm1, pm2));

        Map<UUID, PostContentCacheDTO> result = contentReader.loadFromDatabase(List.of(post1Id));

        assertThat(result).containsKey(post1Id);
        PostContentCacheDTO dto = result.get(post1Id);
        assertThat(dto.isEmpty()).isFalse();
        assertThat(dto.content()).isEqualTo("Post with media");
        assertThat(dto.mediaIds()).containsExactly(media1, media2);
    }

    @Test
    void loadFromDatabase_populatesMissingIdsWithEmptySentinel() {
        UUID existingId = UUID.randomUUID();
        UUID missingId = UUID.randomUUID();

        Post post = new Post();
        post.setId(existingId);
        post.setAuthorId(UUID.randomUUID());
        post.setContent("Existing");
        post.setVisibility(PostVisibility.PUBLIC);
        post.setStatus(PostStatus.ACTIVE);
        post.setCreatedAt(Instant.now());

        when(postRepository.findAllById(List.of(existingId, missingId))).thenReturn(List.of(post));
        when(postMediaRepository.findByPostIdInOrderByDisplayOrderAsc(List.of(existingId, missingId))).thenReturn(List.of());

        Map<UUID, PostContentCacheDTO> result = contentReader.loadFromDatabase(List.of(existingId, missingId));

        assertThat(result).containsKeys(existingId, missingId);
        assertThat(result.get(existingId).isEmpty()).isFalse();
        assertThat(result.get(missingId).isEmpty()).isTrue();
        assertThat(result.get(missingId)).isEqualTo(PostContentCacheDTO.EMPTY);
    }
}
