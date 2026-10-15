package com.minh.fakebook.user.web.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.minh.fakebook.user.IntegrationTest;
import com.minh.fakebook.user.repository.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest @AutoConfigureMockMvc
class SocialContextResourceIT {
    @Autowired MockMvc mvc;
    @MockitoBean FriendshipRepository friends;
    @MockitoBean FollowRepository follows;

    @Test void socialContextRequiresInternalTokenAndInternalCannotBecomeUser() throws Exception {
        UUID user = UUID.randomUUID(), friend = UUID.randomUUID(), following = UUID.randomUUID();
        when(friends.findFriendIdsByUserId(any())).thenReturn(List.of(friend));
        when(follows.findFollowingIdsByUserId(any())).thenReturn(List.of(following));
        String path = "/api/internal/social-context/" + user;
        mvc.perform(get(path)).andExpect(status().isUnauthorized());
        mvc.perform(get(path).with(jwt().jwt(t -> t.subject(user.toString())))).andExpect(status().isForbidden());
        mvc.perform(get(path).with(jwt().jwt(t -> t.subject(user.toString())).authorities(new SimpleGrantedAuthority("ROLE_INTERNAL"))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.friends[0]").value(friend.toString()))
            .andExpect(jsonPath("$.following[0]").value(following.toString()));
        mvc.perform(get("/api/user-profiles/me").with(jwt().jwt(t -> t.subject(user.toString())).authorities(new SimpleGrantedAuthority("ROLE_INTERNAL"))))
            .andExpect(status().isForbidden());
    }
}
