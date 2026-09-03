package com.minh.fakebook.user.service.mapper;

import static com.minh.fakebook.user.domain.FriendRequestAsserts.*;
import static com.minh.fakebook.user.domain.FriendRequestTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FriendRequestMapperTest {

    private FriendRequestMapper friendRequestMapper;

    @BeforeEach
    void setUp() {
        friendRequestMapper = new FriendRequestMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getFriendRequestSample1();
        var actual = friendRequestMapper.toEntity(friendRequestMapper.toDto(expected));
        assertFriendRequestAllPropertiesEquals(expected, actual);
    }
}
