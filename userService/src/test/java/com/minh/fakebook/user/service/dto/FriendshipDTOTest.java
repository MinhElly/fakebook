package com.minh.fakebook.user.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.minh.fakebook.user.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FriendshipDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(FriendshipDTO.class);
        FriendshipDTO friendshipDTO1 = new FriendshipDTO();
        friendshipDTO1.setId(UUID.randomUUID());
        FriendshipDTO friendshipDTO2 = new FriendshipDTO();
        assertThat(friendshipDTO1).isNotEqualTo(friendshipDTO2);
        friendshipDTO2.setId(friendshipDTO1.getId());
        assertThat(friendshipDTO1).isEqualTo(friendshipDTO2);
        friendshipDTO2.setId(UUID.randomUUID());
        assertThat(friendshipDTO1).isNotEqualTo(friendshipDTO2);
        friendshipDTO1.setId(null);
        assertThat(friendshipDTO1).isNotEqualTo(friendshipDTO2);
    }
}
