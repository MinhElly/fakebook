package com.minh.fakebook.media.web.rest;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import com.minh.fakebook.media.IntegrationTest;
import com.minh.fakebook.media.repository.MediaRepository;

@IntegrationTest
@AutoConfigureMockMvc
@Transactional
class AdminMediaResourceIT {
    @Autowired MockMvc mvc;
    @Autowired MediaRepository media;
    @Autowired JdbcTemplate jdbc;
    @Test void archivedMediaCanOnlyBeReadThroughAuditedAdminProxy() throws Exception {
        var asset=MediaResourceIT.createEntity();asset.setUrl("data:image/png;base64,AQID");
        asset=media.saveAndFlush(asset);String id=asset.getId().toString();
        jdbc.update("update media set community_archived=true where id=?",id);
        mvc.perform(get("/api/admin/media/{id}/file",id).with(jwt().jwt(t -> t.subject(UUID.randomUUID().toString())).authorities(new SimpleGrantedAuthority("ROLE_USER"))))
            .andExpect(status().isForbidden());
        assertThat(jdbc.queryForObject("select count(*) from admin_audit where target=?",Long.class,id)).isZero();
        mvc.perform(get("/api/admin/media/{id}/file",id).with(jwt().jwt(t -> t.subject(UUID.randomUUID().toString())).authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
            .andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store")).andExpect(content().bytes(new byte[]{1,2,3}));
        assertThat(jdbc.queryForObject("select count(*) from admin_audit where target=? and action='VIEW_MEDIA'",Long.class,id)).isEqualTo(1);
    }
}
