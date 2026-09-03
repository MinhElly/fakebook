package com.minh.fakebook.user.domain;

import static com.minh.fakebook.user.domain.FriendRequestTestSamples.*;
import static com.minh.fakebook.user.domain.UserProfileTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.minh.fakebook.user.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class FriendRequestTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(FriendRequest.class);
        FriendRequest friendRequest1 = getFriendRequestSample1();
        FriendRequest friendRequest2 = new FriendRequest();
        assertThat(friendRequest1).isNotEqualTo(friendRequest2);

        friendRequest2.setId(friendRequest1.getId());
        assertThat(friendRequest1).isEqualTo(friendRequest2);

        friendRequest2 = getFriendRequestSample2();
        assertThat(friendRequest1).isNotEqualTo(friendRequest2);
    }

    @Test
    void senderTest() {
        FriendRequest friendRequest = getFriendRequestRandomSampleGenerator();
        UserProfile userProfileBack = getUserProfileRandomSampleGenerator();

        friendRequest.setSender(userProfileBack);
        assertThat(friendRequest.getSender()).isEqualTo(userProfileBack);

        friendRequest.sender(null);
        assertThat(friendRequest.getSender()).isNull();
    }

    @Test
    void receiverTest() {
        FriendRequest friendRequest = getFriendRequestRandomSampleGenerator();
        UserProfile userProfileBack = getUserProfileRandomSampleGenerator();

        friendRequest.setReceiver(userProfileBack);
        assertThat(friendRequest.getReceiver()).isEqualTo(userProfileBack);

        friendRequest.receiver(null);
        assertThat(friendRequest.getReceiver()).isNull();
    }
}
