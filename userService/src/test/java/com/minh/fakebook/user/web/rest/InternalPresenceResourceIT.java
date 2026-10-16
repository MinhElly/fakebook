package com.minh.fakebook.user.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.minh.fakebook.user.IntegrationTest;
import com.minh.fakebook.user.client.MediaServiceClient;
import com.minh.fakebook.user.domain.UserProfile;
import com.minh.fakebook.user.repository.UserProfileRepository;
import com.minh.fakebook.user.security.AuthoritiesConstants;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@IntegrationTest
@AutoConfigureMockMvc
class InternalPresenceResourceIT {

    private static final Instant EARLIER =
        Instant.parse("2026-10-16T03:00:00Z");

    private static final Instant LATER =
        Instant.parse("2026-10-16T03:05:00Z");

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @MockitoBean
    private MediaServiceClient mediaServiceClient;

    private UUID userId;

    @Autowired
    private com.minh.fakebook.user.repository.FriendshipRepository friendships;

    @Test
    @org.springframework.transaction.annotation.Transactional
    void readablePresenceChecksBothFriendshipDirectionsAndRevocation() throws Exception {
        UUID friendId = UUID.randomUUID(), strangerId = UUID.randomUUID();
        var friend = userProfileRepository.saveAndFlush(new UserProfile().id(friendId).username("pf-" + friendId)
            .displayName("Friend").createdAt(EARLIER).lastSeenAt(LATER));
        userProfileRepository.saveAndFlush(new UserProfile().id(strangerId).username("ps-" + strangerId)
            .displayName("Stranger").createdAt(EARLIER).lastSeenAt(LATER));
        // A single reverse edge must grant permission too.
        var edge = friendships.saveAndFlush(new com.minh.fakebook.user.domain.Friendship()
            .user(friend).friend(readProfile()).createdAt(EARLIER));
        String body = "{\"viewerId\":\"" + userId + "\",\"userIds\":[\"" + userId + "\",\"" + friendId + "\",\"" + strangerId + "\",\"" + UUID.randomUUID() + "\"]}";
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/internal/presence/readable")
            .with(jwt().authorities(new SimpleGrantedAuthority(AuthoritiesConstants.INTERNAL)))
            .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.length()").value(2))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$[*].userId",
                org.hamcrest.Matchers.containsInAnyOrder(userId.toString(), friendId.toString())));
        friendships.delete(edge); friendships.flush();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/internal/presence/readable")
            .with(jwt().authorities(new SimpleGrantedAuthority(AuthoritiesConstants.INTERNAL)))
            .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.length()").value(1));
    }

    @Test
    void readablePresenceRejectsUnauthorizedInvalidAndMissingViewer() throws Exception {
        String path = "/api/internal/presence/readable";
        String body = "{\"viewerId\":\"" + userId + "\",\"userIds\":[]}";
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(path)
            .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isUnauthorized());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(path)
            .with(jwt().authorities(new SimpleGrantedAuthority(AuthoritiesConstants.USER)))
            .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
        for (String invalid : List.of("{}", "{\"viewerId\":\"" + userId + "\",\"userIds\":[null]}")) {
            mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(path)
                .with(jwt().authorities(new SimpleGrantedAuthority(AuthoritiesConstants.INTERNAL)))
                .contentType(MediaType.APPLICATION_JSON).content(invalid)).andExpect(status().isBadRequest());
        }
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(path)
            .with(jwt().authorities(new SimpleGrantedAuthority(AuthoritiesConstants.INTERNAL)))
            .contentType(MediaType.APPLICATION_JSON).content(body.replace(userId.toString(), UUID.randomUUID().toString())))
            .andExpect(status().isNotFound());
    }

    @BeforeEach
    void createProfile() {
        userId = UUID.randomUUID();

        userProfileRepository.saveAndFlush(
            new UserProfile()
                .id(userId)
                .username("pa-" + userId)
                .displayName("Presence API test")
                .createdAt(EARLIER)
        );
    }

    @AfterEach
    void cleanup() {
        userProfileRepository.deleteById(userId);
    }

    @Test
    void shouldRejectAnonymousUserAndAdmin() throws Exception {
        mvc.perform(
            put(path(userId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload(LATER))
        ).andExpect(status().isUnauthorized());

        for (String authority : List.of(
            AuthoritiesConstants.USER,
            AuthoritiesConstants.ADMIN
        )) {
            mvc.perform(
                put(path(userId))
                    .with(jwt().authorities(
                        new SimpleGrantedAuthority(authority)
                    ))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload(LATER))
            ).andExpect(status().isForbidden());
        }

        assertThat(readProfile().getLastSeenAt()).isNull();
    }

    @Test
    void internalCallerShouldOnlyAdvanceLastSeen() throws Exception {
        internalPut(userId, payload(EARLIER))
            .andExpect(status().isNoContent());

        assertThat(readProfile().getLastSeenAt())
            .isEqualTo(EARLIER);

        internalPut(userId, payload(LATER))
            .andExpect(status().isNoContent());

        assertThat(readProfile().getLastSeenAt())
            .isEqualTo(LATER);

        internalPut(userId, payload(EARLIER))
            .andExpect(status().isNoContent());

        assertThat(readProfile().getLastSeenAt())
            .isEqualTo(LATER);

        internalPut(userId, payload(LATER))
            .andExpect(status().isNoContent());

        assertThat(readProfile().getLastSeenAt())
            .isEqualTo(LATER);
    }

    @Test
    void shouldRejectInvalidBodies() throws Exception {
        for (String body : List.of(
            "{}",
            "{\"lastSeenAt\":null}",
            "{\"lastSeenAt\":\"invalid-time\"}"
        )) {
            internalPut(userId, body)
                .andExpect(status().isBadRequest());
        }

        assertThat(readProfile().getLastSeenAt()).isNull();
    }

    @Test
    void shouldReturnNotFoundForMissingProfile() throws Exception {
        UUID missingId = UUID.randomUUID();

        internalPut(missingId, payload(LATER))
            .andExpect(status().isNotFound());

        assertThat(
            userProfileRepository.existsById(missingId)
        ).isFalse();
    }

    private ResultActions internalPut(
        UUID id,
        String body
    ) throws Exception {
        return mvc.perform(
            put(path(id))
                .with(jwt().authorities(
                    new SimpleGrantedAuthority(
                        AuthoritiesConstants.INTERNAL
                    )
                ))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        );
    }

    private String path(UUID id) {
        return "/api/internal/presence/" + id + "/last-seen";
    }

    private String payload(Instant timestamp) {
        return """
            {"lastSeenAt":"%s"}
            """.formatted(timestamp);
    }

    private UserProfile readProfile() {
        return userProfileRepository.findById(userId)
            .orElseThrow();
    }
}
