package com.minh.fakebook.comment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.minh.fakebook.comment.client.PostFeignClient;
import com.minh.fakebook.comment.domain.PostCache;
import com.minh.fakebook.comment.repository.PostCacheRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class PostCacheResolverTest {

    @Mock
    private PostCacheRepository postCacheRepository;

    @Mock
    private PostFeignClient postFeignClient;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private PostCacheResolver postCacheResolver;

    @Test
    void returnsCachedPostWithoutCallingPostService() {
        UUID postId = UUID.randomUUID();
        PostCache cachedPost = new PostCache().id(postId).authorId(UUID.randomUUID()).status("ACTIVE").visibility("PUBLIC");
        when(postCacheRepository.findById(postId)).thenReturn(Optional.of(cachedPost));

        assertThat(postCacheResolver.resolve(postId)).isSameAs(cachedPost);
        verify(postFeignClient, never()).getPostById(postId);
    }

    @Test
    void fetchesAndCachesMissingPost() {
        UUID postId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        PostFeignClient.PostSyncDTO remotePost = new PostFeignClient.PostSyncDTO(postId, authorId, "ACTIVE", "FRIENDS");
        when(postCacheRepository.findById(postId)).thenReturn(Optional.empty());
        when(postFeignClient.getPostById(postId)).thenReturn(remotePost);

        PostCache resolvedPost = postCacheResolver.resolve(postId);

        assertThat(resolvedPost.getId()).isEqualTo(postId);
        assertThat(resolvedPost.getAuthorId()).isEqualTo(authorId);
        assertThat(resolvedPost.getStatus()).isEqualTo("ACTIVE");
        assertThat(resolvedPost.getVisibility()).isEqualTo("FRIENDS");
        verify(jdbcTemplate)
            .update(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.eq(postId.toString()),
                org.mockito.ArgumentMatchers.eq(authorId.toString()),
                org.mockito.ArgumentMatchers.eq("ACTIVE"),
                org.mockito.ArgumentMatchers.eq("FRIENDS")
            );
    }
}
