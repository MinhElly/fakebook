package com.minh.fakebook.comment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.minh.fakebook.comment.domain.PostCache;
import com.minh.fakebook.comment.repository.PostCacheRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PostCacheResolverTest {

    @Mock
    private PostCacheRepository postCacheRepository;

    @Mock
    private PostCacheWriter postCacheWriter;

    @InjectMocks
    private PostCacheResolver postCacheResolver;

    @Test
    void returnsCachedPostWithoutCallingPostService() {
        UUID postId = UUID.randomUUID();
        PostCache cachedPost = new PostCache().id(postId).authorId(UUID.randomUUID()).status("ACTIVE").visibility("PUBLIC");
        when(postCacheRepository.findById(postId)).thenReturn(Optional.of(cachedPost));

        assertThat(postCacheResolver.resolve(postId)).isSameAs(cachedPost);
        verify(postCacheWriter, never()).fetchAndCache(postId);
    }

    @Test
    void fetchesAndCachesMissingPost() {
        UUID postId = UUID.randomUUID();
        PostCache remotePost = new PostCache().id(postId).authorId(UUID.randomUUID()).status("ACTIVE").visibility("FRIENDS");
        when(postCacheRepository.findById(postId)).thenReturn(Optional.empty());
        when(postCacheWriter.fetchAndCache(postId)).thenReturn(remotePost);

        PostCache resolvedPost = postCacheResolver.resolve(postId);

        assertThat(resolvedPost).isSameAs(remotePost);
        verify(postCacheWriter).fetchAndCache(postId);
    }
}
