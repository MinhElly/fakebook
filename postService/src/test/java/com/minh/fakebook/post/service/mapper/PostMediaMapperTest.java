package com.minh.fakebook.post.service.mapper;

import static com.minh.fakebook.post.domain.PostMediaAsserts.*;
import static com.minh.fakebook.post.domain.PostMediaTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PostMediaMapperTest {

    private PostMediaMapper postMediaMapper;

    @BeforeEach
    void setUp() {
        postMediaMapper = new PostMediaMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getPostMediaSample1();
        var actual = postMediaMapper.toEntity(postMediaMapper.toDto(expected));
        assertPostMediaAllPropertiesEquals(expected, actual);
    }
}
