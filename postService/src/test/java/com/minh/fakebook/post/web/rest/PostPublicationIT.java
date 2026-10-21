package com.minh.fakebook.post.web.rest;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.minh.fakebook.post.IntegrationTest;
import com.minh.fakebook.post.client.*;
import com.minh.fakebook.post.client.dto.MediaValidationDTO;
import com.minh.fakebook.post.service.PostPublicationService;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

/** No ambient test transaction: exercises real commit, rollback, and concurrent decisions. */
@IntegrationTest @AutoConfigureMockMvc
class PostPublicationIT {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;
    @Autowired PostPublicationService service;
    @Autowired org.springframework.transaction.PlatformTransactionManager transactions;
    @MockitoBean MediaServiceClient media;
    @MockitoBean UserServiceClient users;
    final UUID author=UUID.randomUUID(), id=UUID.randomUUID();
    @AfterEach void cleanup() {
        new org.springframework.transaction.support.TransactionTemplate(transactions).executeWithoutResult(tx -> jdbc.update("UPDATE posts SET status='DELETED' WHERE author_id=?",author.toString()));
    }
    String draft() { return "{\"content\":\"publication integration\",\"visibility\":\"PUBLIC\",\"taggedUserIds\":[],\"targetPostId\":null,\"version\":null}"; }
    void begin() throws Exception {
        mvc.perform(put("/api/posts/publications/{id}",id).with(jwt().jwt(j->j.subject(author.toString())))
            .contentType(MediaType.APPLICATION_JSON).content(draft())).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PENDING"));
    }
    String publish() throws Exception {
        return mvc.perform(post("/api/posts/publications/{id}/publish",id).with(jwt().jwt(j->j.subject(author.toString())))
            .contentType(MediaType.APPLICATION_JSON).content("{\"mediaIds\":[]}")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PUBLISHED")).andReturn().getResponse().getContentAsString();
    }
    long posts() { return jdbc.queryForObject("SELECT COUNT(*) FROM posts WHERE author_id=?",Long.class,author.toString()); }
    @Test void publicationAndPostCommitOnceAndReplayAfterLostResponse() throws Exception {
        begin(); begin(); assertThat(posts()).isZero();
        String first=publish(), replay=publish();
        assertThat(json.readTree(first).get("post").get("id")).isEqualTo(json.readTree(replay).get("post").get("id"));
        assertThat(posts()).isEqualTo(1);
        mvc.perform(delete("/api/posts/publications/{id}",id).with(jwt().jwt(j->j.subject(author.toString()))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PUBLISHED"));
        assertThat(posts()).isEqualTo(1);
    }
    @Test void failedValidationRollsBackPostAndDurablyCancelsSession() throws Exception {
        begin();UUID file=UUID.randomUUID();when(media.getMedias(any())).thenReturn(List.of());
        mvc.perform(post("/api/posts/publications/{id}/publish",id).with(jwt().jwt(j->j.subject(author.toString())))
            .contentType(MediaType.APPLICATION_JSON).content("{\"mediaIds\":[\""+file+"\"]}")).andExpect(status().isForbidden());
        assertThat(posts()).isZero();
        assertThat(jdbc.queryForObject("SELECT status FROM post_publication WHERE id=?",String.class,id.toString())).isEqualTo("CANCELLED");
    }
    @Test void cancelledAndExpiredSessionsCannotPublish() throws Exception {
        begin();mvc.perform(delete("/api/posts/publications/{id}",id).with(jwt().jwt(j->j.subject(author.toString())))).andExpect(status().isOk());
        mvc.perform(post("/api/posts/publications/{id}/publish",id).with(jwt().jwt(j->j.subject(author.toString())))
            .contentType(MediaType.APPLICATION_JSON).content("{\"mediaIds\":[]}")).andExpect(status().isGone());
        assertThat(posts()).isZero();
    }
    @Test void ownerAndDraftAreBoundToIdempotencyKey() throws Exception {
        begin();
        mvc.perform(get("/api/posts/publications/{id}",id).with(jwt().jwt(j->j.subject(UUID.randomUUID().toString())))).andExpect(status().isForbidden());
        mvc.perform(put("/api/posts/publications/{id}",id).with(jwt().jwt(j->j.subject(author.toString())))
            .contentType(MediaType.APPLICATION_JSON).content(draft().replace("integration","changed"))).andExpect(status().isConflict());
        assertThat(posts()).isZero();
    }
    @Test void concurrentPublishRequestsProduceOnePost() throws Exception {
        begin();var executor=Executors.newFixedThreadPool(2);
        try { var a=executor.submit(()->publish());var b=executor.submit(()->publish());
            assertThat(json.readTree(a.get(40,TimeUnit.SECONDS)).get("post").get("id"))
                .isEqualTo(json.readTree(b.get(40,TimeUnit.SECONDS)).get("post").get("id"));
            assertThat(posts()).isEqualTo(1);
        } finally { executor.shutdownNow(); }
    }
    @Test void expiredAbandonedPublicationIsCancelledWithoutFrontend() throws Exception {
        begin();new org.springframework.transaction.support.TransactionTemplate(transactions).executeWithoutResult(tx -> jdbc.update("UPDATE post_publication SET expires_at=? WHERE id=?",java.time.Instant.now().minusSeconds(1).getEpochSecond(),id.toString()));
        service.expire();assertThat(jdbc.queryForObject("SELECT status FROM post_publication WHERE id=?",String.class,id.toString())).isEqualTo("CANCELLED");
        assertThat(posts()).isZero();
    }
    @Test void temporaryMediaCannotBypassPublicationThroughLegacyCreate() throws Exception {
        UUID file=UUID.randomUUID();when(media.getMedias(any())).thenReturn(List.of(new MediaValidationDTO(file,author,"ACTIVE","POST",id)));
        mvc.perform(post("/api/posts/create").with(jwt().jwt(j->j.subject(author.toString()))).contentType(MediaType.APPLICATION_JSON)
            .content("{\"content\":\"bypass\",\"visibility\":\"PUBLIC\",\"mediaIds\":[\""+file+"\"],\"taggedUserIds\":[]}")).andExpect(status().isForbidden());
        assertThat(posts()).isZero();
    }
    @Test void cancelledOrPublishedDecisionWinsConcurrentRaceWithoutOrphanPost() throws Exception {
        begin();var executor=Executors.newFixedThreadPool(2);
        try {
            var a=executor.submit(()->mvc.perform(post("/api/posts/publications/{id}/publish",id).with(jwt().jwt(j->j.subject(author.toString())))
                .contentType(MediaType.APPLICATION_JSON).content("{\"mediaIds\":[]}")).andReturn().getResponse().getStatus());
            var b=executor.submit(()->mvc.perform(delete("/api/posts/publications/{id}",id).with(jwt().jwt(j->j.subject(author.toString())))).andReturn().getResponse().getStatus());
            assertThat(a.get(40,TimeUnit.SECONDS)).isIn(200,410);assertThat(b.get(40,TimeUnit.SECONDS)).isEqualTo(200);
            String decision=jdbc.queryForObject("SELECT status FROM post_publication WHERE id=?",String.class,id.toString());
            assertThat(posts()).isEqualTo(decision.equals("PUBLISHED")?1:0);
        } finally { executor.shutdownNow(); }
    }
    @Test void boundMediaCanOnlyCommitThroughMatchingPublication() throws Exception {
        begin();UUID file=UUID.randomUUID();when(media.getMedias(any())).thenReturn(List.of(new MediaValidationDTO(file,author,"ACTIVE","POST",id)));
        mvc.perform(post("/api/posts/publications/{id}/publish",id).with(jwt().jwt(j->j.subject(author.toString())))
            .contentType(MediaType.APPLICATION_JSON).content("{\"mediaIds\":[\""+file+"\"]}")).andExpect(status().isOk()).andExpect(jsonPath("$.post.mediaIds[0]").value(file.toString()));
        assertThat(posts()).isEqualTo(1);
    }

    @Test void editPublicationReplaysSameVersionWithoutApplyingEditTwice() throws Exception {
        begin();var original=json.readTree(publish()).get("post");UUID editId=UUID.randomUUID();
        String body=json.writeValueAsString(Map.of("content","updated through publication","visibility","PUBLIC","taggedUserIds",List.of(),"targetPostId",original.get("id").asText(),"version",original.get("version").asLong()));
        mvc.perform(put("/api/posts/publications/{id}",editId).with(jwt().jwt(j->j.subject(author.toString())))
            .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk());
        for(int i=0;i<2;i++) mvc.perform(post("/api/posts/publications/{id}/publish",editId).with(jwt().jwt(j->j.subject(author.toString())))
            .contentType(MediaType.APPLICATION_JSON).content("{\"mediaIds\":[]}")).andExpect(status().isOk())
            .andExpect(jsonPath("$.post.id").value(original.get("id").asText())).andExpect(jsonPath("$.post.content").value("updated through publication"));
        assertThat(posts()).isEqualTo(1);
    }

}
