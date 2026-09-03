package com.minh.fakebook.user.domain;

import static com.minh.fakebook.user.domain.FollowTestSamples.*;
import static com.minh.fakebook.user.domain.UserProfileTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.minh.fakebook.user.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class FollowTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Follow.class);
        Follow follow1 = getFollowSample1();
        Follow follow2 = new Follow();
        assertThat(follow1).isNotEqualTo(follow2);

        follow2.setId(follow1.getId());
        assertThat(follow1).isEqualTo(follow2);

        follow2 = getFollowSample2();
        assertThat(follow1).isNotEqualTo(follow2);
    }

    @Test
    void followerTest() {
        Follow follow = getFollowRandomSampleGenerator();
        UserProfile userProfileBack = getUserProfileRandomSampleGenerator();

        follow.setFollower(userProfileBack);
        assertThat(follow.getFollower()).isEqualTo(userProfileBack);

        follow.follower(null);
        assertThat(follow.getFollower()).isNull();
    }

    @Test
    void followingTest() {
        Follow follow = getFollowRandomSampleGenerator();
        UserProfile userProfileBack = getUserProfileRandomSampleGenerator();

        follow.setFollowing(userProfileBack);
        assertThat(follow.getFollowing()).isEqualTo(userProfileBack);

        follow.following(null);
        assertThat(follow.getFollowing()).isNull();
    }
}
