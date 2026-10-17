package com.minh.fakebook.feed.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.*;
import com.minh.fakebook.feed.client.*;
import com.minh.fakebook.feed.domain.FeedItem;
import com.minh.fakebook.feed.repository.FeedItemRepository;
import com.minh.fakebook.feed.service.mapper.FeedItemMapper;
import org.mapstruct.factory.Mappers;

class AuthorizedUserFeedServiceTest {
    @Test void excludesStaleHiddenCandidatesBeforeExternalPagination() {
        UUID viewer=UUID.randomUUID(),blocked=UUID.randomUUID(),visible=UUID.randomUUID();
        var repository=mock(FeedItemRepository.class);var posts=mock(FeedReadClient.class);var social=mock(SocialContextClient.class);
        var first=new FeedItem().userId(viewer).postId(blocked).authorId(UUID.randomUUID()).createdAt(Instant.now());
        var second=new FeedItem().userId(viewer).postId(visible).authorId(UUID.randomUUID()).createdAt(Instant.now());
        when(repository.findByUserIdOrderByCreatedAtDescPostIdDesc(eq(viewer),any())).thenReturn(new PageImpl<>(List.of(first,second)));
        when(social.read(viewer)).thenReturn(new SocialContextClient.SocialContext(Set.of(),Set.of()));
        when(posts.readable(any())).thenReturn(List.of(new FeedReadClient.Candidate(visible,second.getAuthorId(),"PUBLIC",second.getCreatedAt())));
        var service=new AuthorizedUserFeedService(repository,Mappers.getMapper(FeedItemMapper.class),posts,social);
        var result=service.getUserFeed(viewer,PageRequest.of(0,1));
        assertThat(result.getTotalElements()).isEqualTo(1);assertThat(result.getContent()).extracting(dto->dto.getPostId()).containsExactly(visible);
    }
}
