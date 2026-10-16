package com.minh.fakebook.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.minh.fakebook.post.domain.enumeration.ReactionType;
import com.minh.fakebook.post.repository.PostReactionRepository;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PostReactionCountsReaderTest {

    private PostReactionRepository postReactionRepository;
    private PostReactionCountsReader reader;

    @BeforeEach
    void setUp() {
        postReactionRepository = mock(PostReactionRepository.class);
        reader = new PostReactionCountsReader(postReactionRepository);
    }

    @Test
    void loadFromDatabase_withEmptyOrNull_returnsEmptyMap() {
        assertThat(reader.loadFromDatabase(null)).isEmpty();
        assertThat(reader.loadFromDatabase(List.of())).isEmpty();
        verifyNoInteractions(postReactionRepository);
    }

    @Test
    void loadFromDatabase_withPostHavingZeroReactions_initializesAllTypesToZero() {
        UUID postId = UUID.randomUUID();
        when(postReactionRepository.countByPostIds(List.of(postId))).thenReturn(List.of());

        Map<UUID, Map<ReactionType, Long>> result = reader.loadFromDatabase(List.of(postId));

        assertThat(result).containsKey(postId);
        Map<ReactionType, Long> counts = result.get(postId);
        assertThat(counts).hasSize(ReactionType.values().length);
        for (ReactionType type : ReactionType.values()) {
            assertThat(counts.get(type)).isZero();
        }
        verify(postReactionRepository).countByPostIds(List.of(postId));
    }

    @Test
    void loadFromDatabase_withReactions_populatesCountsCorrectly() {
        UUID post1 = UUID.randomUUID();
        UUID post2 = UUID.randomUUID();

        PostReactionRepository.ReactionCountProjection proj1 = createProjection(post1, ReactionType.LIKE, 5L);
        PostReactionRepository.ReactionCountProjection proj2 = createProjection(post1, ReactionType.LOVE, 2L);
        PostReactionRepository.ReactionCountProjection proj3 = createProjection(post2, ReactionType.HAHA, 10L);

        when(postReactionRepository.countByPostIds(List.of(post1, post2)))
            .thenReturn(List.of(proj1, proj2, proj3));

        Map<UUID, Map<ReactionType, Long>> result = reader.loadFromDatabase(List.of(post1, post2));

        assertThat(result).containsOnlyKeys(post1, post2);

        Map<ReactionType, Long> counts1 = result.get(post1);
        assertThat(counts1.get(ReactionType.LIKE)).isEqualTo(5L);
        assertThat(counts1.get(ReactionType.LOVE)).isEqualTo(2L);
        assertThat(counts1.get(ReactionType.HAHA)).isZero();
        assertThat(counts1.get(ReactionType.WOW)).isZero();
        assertThat(counts1.get(ReactionType.SAD)).isZero();
        assertThat(counts1.get(ReactionType.ANGRY)).isZero();

        Map<ReactionType, Long> counts2 = result.get(post2);
        assertThat(counts2.get(ReactionType.HAHA)).isEqualTo(10L);
        assertThat(counts2.get(ReactionType.LIKE)).isZero();
    }

    private PostReactionRepository.ReactionCountProjection createProjection(
        UUID postId,
        ReactionType reactionType,
        long count
    ) {
        return new PostReactionRepository.ReactionCountProjection() {
            @Override
            public UUID getPostId() {
                return postId;
            }

            @Override
            public ReactionType getReactionType() {
                return reactionType;
            }

            @Override
            public long getReactionCount() {
                return count;
            }
        };
    }
}
