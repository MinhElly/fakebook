package com.minh.fakebook.comment.service.mapper;

import static com.minh.fakebook.comment.domain.CommentReactionAsserts.*;
import static com.minh.fakebook.comment.domain.CommentReactionTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CommentReactionMapperTest {

    private CommentReactionMapper commentReactionMapper;

    @BeforeEach
    void setUp() {
        commentReactionMapper = new CommentReactionMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getCommentReactionSample1();
        var actual = commentReactionMapper.toEntity(commentReactionMapper.toDto(expected));
        assertCommentReactionAllPropertiesEquals(expected, actual);
    }
}
