package com.minh.fakebook.user.service.mapper;

import static com.minh.fakebook.user.domain.FriendshipAsserts.*;
import static com.minh.fakebook.user.domain.FriendshipTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FriendshipMapperTest {

    private FriendshipMapper friendshipMapper;

    @BeforeEach
    void setUp() {
        friendshipMapper = new FriendshipMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getFriendshipSample1();
        var actual = friendshipMapper.toEntity(friendshipMapper.toDto(expected));
        assertFriendshipAllPropertiesEquals(expected, actual);
    }
}
