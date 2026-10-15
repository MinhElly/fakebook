package com.minh.fakebook.comment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.minh.fakebook.comment.domain.Comment;
import com.minh.fakebook.comment.domain.enumeration.CommentStatus;
import com.minh.fakebook.comment.repository.CommentRepository;
import com.minh.fakebook.comment.service.dto.CommentSummaryCacheDTO;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CommentSummaryReaderTest {

    private CommentRepository commentRepository;
    private CommentSummaryReader reader;

    @BeforeEach
    void setUp() {
        commentRepository = mock(CommentRepository.class);
        reader = new CommentSummaryReader(commentRepository);
    }

    @Test
    void shouldReturnEmptyMapWhenPostIdsIsEmptyOrNull() {
        assertThat(reader.loadFromDatabase(null)).isEmpty();
        assertThat(reader.loadFromDatabase(List.of())).isEmpty();
        verifyNoInteractions(commentRepository);
    }

    @Test
    void shouldReturnZeroCountAndNullPreviewWhenNoCommentsExist() {
        UUID postId = UUID.randomUUID();
        when(commentRepository.countByPostIdsAndStatus(List.of(postId), CommentStatus.ACTIVE))
            .thenReturn(List.of());
        when(commentRepository.findLatestActiveRootComments(List.of(postId)))
            .thenReturn(List.of());

        Map<UUID, CommentSummaryCacheDTO> result = reader.loadFromDatabase(List.of(postId));

        assertThat(result).hasSize(1);
        assertThat(result.get(postId)).isNotNull();
        assertThat(result.get(postId).commentCount()).isEqualTo(0L);
        assertThat(result.get(postId).previewComment()).isNull();
    }

    @Test
    void shouldReturnCorrectCountAndLatestActiveRootCommentPreview() {
        UUID postId = UUID.randomUUID();
        UUID commentId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-10-15T12:00:00Z");

        CommentRepository.CommentCountProjection projection = mock(CommentRepository.CommentCountProjection.class);
        when(projection.getPostId()).thenReturn(postId);
        when(projection.getCommentCount()).thenReturn(7L);

        Comment preview = new Comment()
            .postId(postId)
            .authorId(authorId)
            .content("Hello World")
            .status(CommentStatus.ACTIVE);
        preview.setId(commentId);
        preview.setCreatedAt(createdAt);

        when(commentRepository.countByPostIdsAndStatus(List.of(postId), CommentStatus.ACTIVE))
            .thenReturn(List.of(projection));
        when(commentRepository.findLatestActiveRootComments(List.of(postId)))
            .thenReturn(List.of(preview));

        Map<UUID, CommentSummaryCacheDTO> result = reader.loadFromDatabase(List.of(postId));

        assertThat(result).hasSize(1);
        CommentSummaryCacheDTO dto = result.get(postId);
        assertThat(dto).isNotNull();
        assertThat(dto.commentCount()).isEqualTo(7L);
        assertThat(dto.previewComment()).isNotNull();
        assertThat(dto.previewComment().id()).isEqualTo(commentId);
        assertThat(dto.previewComment().authorId()).isEqualTo(authorId);
        assertThat(dto.previewComment().content()).isEqualTo("Hello World");
        assertThat(dto.previewComment().createdAt()).isEqualTo(createdAt);
    }

    @Test
    void shouldHandleMultiplePostIdsWithMixedResults() {
        UUID post1 = UUID.randomUUID();
        UUID post2 = UUID.randomUUID();

        CommentRepository.CommentCountProjection projection1 = mock(CommentRepository.CommentCountProjection.class);
        when(projection1.getPostId()).thenReturn(post1);
        when(projection1.getCommentCount()).thenReturn(3L);

        Comment preview1 = new Comment()
            .postId(post1)
            .authorId(UUID.randomUUID())
            .content("Latest root on post 1")
            .status(CommentStatus.ACTIVE);
        preview1.setId(UUID.randomUUID());
        preview1.setCreatedAt(Instant.now());

        when(commentRepository.countByPostIdsAndStatus(List.of(post1, post2), CommentStatus.ACTIVE))
            .thenReturn(List.of(projection1));
        when(commentRepository.findLatestActiveRootComments(List.of(post1, post2)))
            .thenReturn(List.of(preview1));

        Map<UUID, CommentSummaryCacheDTO> result = reader.loadFromDatabase(List.of(post1, post2));

        assertThat(result).hasSize(2);
        assertThat(result.get(post1).commentCount()).isEqualTo(3L);
        assertThat(result.get(post1).previewComment()).isNotNull();
        assertThat(result.get(post2).commentCount()).isEqualTo(0L);
        assertThat(result.get(post2).previewComment()).isNull();
    }
}
