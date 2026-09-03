package com.minh.fakebook.post.service.mapper;

import static com.minh.fakebook.post.domain.PostAsserts.*;
import static com.minh.fakebook.post.domain.PostTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PostMapperTest {

    private PostMapper postMapper;

    @BeforeEach
    void setUp() {
        postMapper = new PostMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getPostSample1();
        var actual = postMapper.toEntity(postMapper.toDto(expected));
        assertPostAllPropertiesEquals(expected, actual);
    }
}
