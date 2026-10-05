package com.minh.fakebook.comment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.minh.fakebook.comment.client.PostFeignClient;
import com.minh.fakebook.comment.domain.PostCache;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class PostCacheWriterTest {

    @Mock
    private PostFeignClient postFeignClient;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private PostCacheWriter postCacheWriter;

    @Test
    void fetchesAndCachesMissingPost() {
        UUID postId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        PostFeignClient.PostSyncDTO remotePost = new PostFeignClient.PostSyncDTO(postId, authorId, "ACTIVE", "FRIENDS");
        when(postFeignClient.getPostById(postId)).thenReturn(remotePost);

        PostCache resolvedPost = postCacheWriter.fetchAndCache(postId);

        assertThat(resolvedPost.getId()).isEqualTo(postId);
        assertThat(resolvedPost.getAuthorId()).isEqualTo(authorId);
        assertThat(resolvedPost.getStatus()).isEqualTo("ACTIVE");
        assertThat(resolvedPost.getVisibility()).isEqualTo("FRIENDS");
        verify(jdbcTemplate).update(anyString(), eq(postId.toString()), eq(authorId.toString()), eq("ACTIVE"), eq("FRIENDS"));
    }

    @Test
    void rejectsMissingRemotePost() {
        UUID postId = UUID.randomUUID();
        when(postFeignClient.getPostById(postId)).thenReturn(null);

        assertThatThrownBy(() -> postCacheWriter.fetchAndCache(postId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Post not found: " + postId);
    }
}
