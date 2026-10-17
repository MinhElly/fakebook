package com.minh.fakebook.post.web.rest;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import com.minh.fakebook.post.IntegrationTest;
import com.minh.fakebook.post.domain.Post;
import com.minh.fakebook.post.domain.enumeration.*;
import com.minh.fakebook.post.repository.PostRepository;

@IntegrationTest
@AutoConfigureMockMvc
@Transactional
class AdminContentResourceIT {
    @Autowired MockMvc mvc;
    @Autowired PostRepository posts;
    @Autowired JdbcTemplate jdbc;
    @Test void adminDetailAndModerationPersistAuditAndOutboxAndRejectUser() throws Exception {
        var post=posts.saveAndFlush(new Post().authorId(UUID.randomUUID()).content("private moderation fixture")
            .status(PostStatus.ACTIVE).visibility(PostVisibility.PRIVATE).createdAt(Instant.now()));
        String admin=UUID.randomUUID().toString();
        mvc.perform(get("/api/admin/posts").with(jwt().jwt(t -> t.subject(admin)).authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
            .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("private moderation fixture"))));
        mvc.perform(get("/api/admin/posts/{id}",post.getId()).with(jwt().jwt(t -> t.subject(admin)).authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.content").value("private moderation fixture"));
        assertThat(jdbc.queryForObject("select count(*) from admin_audit where target=? and action='VIEW_POST'",Long.class,post.getId().toString())).isEqualTo(1);
        mvc.perform(post("/api/admin/posts/{id}/hide",post.getId()).with(jwt().jwt(t -> t.subject(admin)).authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
            .header("Idempotency-Key","moderate-"+post.getId()).contentType("application/json").content("{\"version\":"+post.getVersion()+",\"reason\":\"verified violation\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.moderation_visibility").value("HIDDEN"));
        assertThat(jdbc.queryForObject("select count(*) from admin_audit where target=?",Long.class,post.getId().toString())).isEqualTo(2);
        assertThat(jdbc.queryForObject("select content from posts where id=?",String.class,post.getId().toString())).isEqualTo("private moderation fixture");
        mvc.perform(get("/api/admin/posts/{id}",post.getId()).with(jwt().jwt(t -> t.subject(UUID.randomUUID().toString())).authorities(new SimpleGrantedAuthority("ROLE_USER"))))
            .andExpect(status().isForbidden());
    }
}
