package com.minh.fakebook.feed.web.rest;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.minh.fakebook.feed.IntegrationTest;
import com.minh.fakebook.feed.service.HomeFeedService;
import jakarta.servlet.http.Cookie;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest @AutoConfigureMockMvc
class HomeFeedResourceIT {
    @Autowired MockMvc mvc;
    @MockitoBean HomeFeedService feeds;
    @Autowired org.springframework.security.oauth2.jwt.JwtDecoder decoder;

    @Test void homeAndFriendsRequireAuthentication() throws Exception {
        mvc.perform(get("/api/feed/home")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/feed/friends")).andExpect(status().isUnauthorized());
        verifyNoInteractions(feeds);
    }

    @Test void invalidJwtNeverUsesGuestAndFriendsUsesJwtSubject() throws Exception {
        when(decoder.decode("bad-jwt")).thenThrow(new org.springframework.security.oauth2.jwt.BadJwtException("invalid"));
        mvc.perform(get("/api/feed/home").header("Authorization","Bearer bad-jwt")).andExpect(status().isUnauthorized());
        verifyNoInteractions(feeds);
        UUID user = UUID.randomUUID();
        when(feeds.get(eq(user),eq("friends"),isNull())).thenReturn(new HomeFeedService.Page(List.of(),null,false));
        mvc.perform(get("/api/feed/friends").with(jwt().jwt(t -> t.subject(user.toString())))).andExpect(status().isOk());
        verify(feeds).get(user,"friends",null);
    }
}
