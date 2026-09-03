package com.minh.fakebook.post.service.mapper;

import static com.minh.fakebook.post.domain.PostReactionAsserts.*;
import static com.minh.fakebook.post.domain.PostReactionTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PostReactionMapperTest {

    private PostReactionMapper postReactionMapper;

    @BeforeEach
    void setUp() {
        postReactionMapper = new PostReactionMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getPostReactionSample1();
        var actual = postReactionMapper.toEntity(postReactionMapper.toDto(expected));
        assertPostReactionAllPropertiesEquals(expected, actual);
    }
}
