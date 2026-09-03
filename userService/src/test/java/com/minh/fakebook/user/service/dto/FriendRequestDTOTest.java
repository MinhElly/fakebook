package com.minh.fakebook.user.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.minh.fakebook.user.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FriendRequestDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(FriendRequestDTO.class);
        FriendRequestDTO friendRequestDTO1 = new FriendRequestDTO();
        friendRequestDTO1.setId(UUID.randomUUID());
        FriendRequestDTO friendRequestDTO2 = new FriendRequestDTO();
        assertThat(friendRequestDTO1).isNotEqualTo(friendRequestDTO2);
        friendRequestDTO2.setId(friendRequestDTO1.getId());
        assertThat(friendRequestDTO1).isEqualTo(friendRequestDTO2);
        friendRequestDTO2.setId(UUID.randomUUID());
        assertThat(friendRequestDTO1).isNotEqualTo(friendRequestDTO2);
        friendRequestDTO1.setId(null);
        assertThat(friendRequestDTO1).isNotEqualTo(friendRequestDTO2);
    }
}
