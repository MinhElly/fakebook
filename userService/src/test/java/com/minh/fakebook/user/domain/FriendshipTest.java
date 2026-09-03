package com.minh.fakebook.user.domain;

import static com.minh.fakebook.user.domain.FriendshipTestSamples.*;
import static com.minh.fakebook.user.domain.UserProfileTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.minh.fakebook.user.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class FriendshipTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Friendship.class);
        Friendship friendship1 = getFriendshipSample1();
        Friendship friendship2 = new Friendship();
        assertThat(friendship1).isNotEqualTo(friendship2);

        friendship2.setId(friendship1.getId());
        assertThat(friendship1).isEqualTo(friendship2);

        friendship2 = getFriendshipSample2();
        assertThat(friendship1).isNotEqualTo(friendship2);
    }

    @Test
    void userTest() {
        Friendship friendship = getFriendshipRandomSampleGenerator();
        UserProfile userProfileBack = getUserProfileRandomSampleGenerator();

        friendship.setUser(userProfileBack);
        assertThat(friendship.getUser()).isEqualTo(userProfileBack);

        friendship.user(null);
        assertThat(friendship.getUser()).isNull();
    }

    @Test
    void friendTest() {
        Friendship friendship = getFriendshipRandomSampleGenerator();
        UserProfile userProfileBack = getUserProfileRandomSampleGenerator();

        friendship.setFriend(userProfileBack);
        assertThat(friendship.getFriend()).isEqualTo(userProfileBack);

        friendship.friend(null);
        assertThat(friendship.getFriend()).isNull();
    }
}
