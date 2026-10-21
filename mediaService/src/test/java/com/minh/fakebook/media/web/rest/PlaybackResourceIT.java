package com.minh.fakebook.media.web.rest;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import com.minh.fakebook.media.IntegrationTest;
import com.minh.fakebook.media.domain.Media;
import com.minh.fakebook.media.domain.enumeration.*;
import com.minh.fakebook.media.repository.MediaRepository;
import com.minh.fakebook.media.service.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@IntegrationTest
@AutoConfigureMockMvc
class PlaybackResourceIT {
    @Autowired MockMvc mvc;
    @Autowired MediaRepository media;
    @Autowired JwtDecoder decoder;
    @Autowired ObjectMapper json;
    @MockitoBean PostMediaAccess access;
    @MockitoBean MediaFileProxy proxy;

    @Test @Transactional void ticketRequiresJwtAndRechecksParentPermissionOnEachRangeRequest() throws Exception {
        UUID owner = UUID.randomUUID();
        Media item = media.saveAndFlush(new Media().ownerId(owner).fileName("test.mp4").mediaType(MediaType.VIDEO)
            .mimeType("video/mp4").fileSize(100L).storageProvider(StorageProvider.CLOUDINARY).storageKey("test")
            .url("https://res.cloudinary.com/test/video/upload/test.mp4").status(MediaStatus.ACTIVE).createdAt(Instant.now()));
        item.setPurpose(MediaPurpose.POST); media.flush();
        when(access.canOwnerRead(item.getId())).thenReturn(true);
        Jwt token = Jwt.withTokenValue("playback-test-token").header("alg", "RS256").subject(owner.toString())
            .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(600)).claim("roles",List.of("ROLE_USER")).build();
        when(decoder.decode("playback-test-token")).thenReturn(token);
        String base = "/api/media/" + item.getId();
        mvc.perform(post(base + "/playback")).andExpect(status().isUnauthorized());
        String response = mvc.perform(post(base + "/playback").with(jwt().jwt(token)))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String ticket = json.readTree(response).get("ticket").asString();
        assertThat(response).doesNotContain("playback-test-token");
        mvc.perform(get(base + "/file")).andExpect(status().isUnauthorized());
        mvc.perform(get(base + "/file").param("ticket", "invalid")).andExpect(status().isUnauthorized());
        doReturn(ResponseEntity.status(206).header("Content-Range", "bytes 10-12/100")
            .body(new InputStreamResource(new java.io.ByteArrayInputStream(new byte[]{1,2,3}))))
            .when(proxy).fetch(anyString(), eq("bytes=10-12"));
        mvc.perform(get(base + "/file").param("ticket",ticket).header("Range","bytes=10-12"))
            .andExpect(status().isPartialContent()).andExpect(header().string("Content-Range","bytes 10-12/100"));
        when(access.canOwnerRead(item.getId())).thenReturn(false);
        mvc.perform(get(base + "/file").param("ticket",ticket).header("Range","bytes=20-30"))
            .andExpect(status().isForbidden());
        verify(proxy, times(1)).fetch(anyString(), anyString());
    }
}
