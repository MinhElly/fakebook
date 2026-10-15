package com.minh.fakebook.post.web.rest;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.minh.fakebook.post.IntegrationTest;
import com.minh.fakebook.post.client.*;
import com.minh.fakebook.post.client.dto.MediaValidationDTO;
import com.minh.fakebook.post.domain.Post;
import com.minh.fakebook.post.domain.enumeration.*;
import com.minh.fakebook.post.repository.PostRepository;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@IntegrationTest @AutoConfigureMockMvc @Transactional
class PostTimelineIT {
    @Autowired MockMvc mvc;
    @Autowired PostRepository posts;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean MediaServiceClient media;
    @MockitoBean UserServiceClient users;
    final UUID author = UUID.randomUUID(), friend = UUID.randomUUID(), outsider = UUID.randomUUID();
    final Instant published = Instant.parse("2025-01-01T00:00:00Z");
    Post post;
    @BeforeEach void setup() {
        post = posts.saveAndFlush(new Post().authorId(author).content("original").visibility(PostVisibility.PUBLIC).status(PostStatus.ACTIVE).createdAt(published));
        when(users.getFriendIdsByUserId(any())).thenReturn(List.of(friend));
        when(users.areFriends(any(), any())).thenReturn(true);
    }
    String body(Map<String,Object> changes) throws Exception {
        var body = new HashMap<String,Object>(changes); body.put("id", post.getId());
        body.putIfAbsent("version", posts.findById(post.getId()).orElseThrow().getVersion());
        return json.writeValueAsString(body);
    }
    long revisions() { return jdbc.queryForObject("SELECT COUNT(*) FROM post_revision WHERE post_id = ?", Long.class, post.getId().toString()); }
    @Test void addKeepRemoveMediaAndHistoryPreservesTimestamp() throws Exception {
        UUID image = UUID.randomUUID();
        when(media.getMedia(image)).thenReturn(new MediaValidationDTO(image, author, "ACTIVE", "POST"));
        mvc.perform(patch("/api/posts/{id}",post.getId()).with(jwt().jwt(t -> t.subject(author.toString()))).with(csrf())
            .contentType(MediaType.APPLICATION_JSON).content(body(Map.of("mediaIds",List.of(image)))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.mediaIds[0]").value(image.toString())).andExpect(jsonPath("$.createdAt").value(published.toString()));
        assertThat(revisions()).isEqualTo(2);
        mvc.perform(patch("/api/posts/{id}",post.getId()).with(jwt().jwt(t -> t.subject(author.toString()))).with(csrf())
            .contentType(MediaType.APPLICATION_JSON).content(body(Map.of("content","edited"))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.mediaIds[0]").value(image.toString()));
        mvc.perform(patch("/api/posts/{id}",post.getId()).with(jwt().jwt(t -> t.subject(author.toString()))).with(csrf())
            .contentType(MediaType.APPLICATION_JSON).content(body(Map.of("mediaIds",List.of()))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.mediaIds").isEmpty());
        mvc.perform(get("/api/posts/{id}/history",post.getId()).with(jwt().jwt(t -> t.subject(outsider.toString())))).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(4));
    }
    @Test void missingVersionCannotOverwritePost() throws Exception {
        mvc.perform(patch("/api/posts/{id}", post.getId()).with(jwt().jwt(t -> t.subject(author.toString())))
            .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("id", post.getId(), "content", "stale editor"))))
            .andExpect(status().isBadRequest());
        assertThat(posts.findById(post.getId()).orElseThrow().getContent()).isEqualTo("original");
        assertThat(revisions()).isZero();
    }

    @Test void adminCanAttachOwnUploadAndAuthorCanRetainIt() throws Exception {
        UUID image = UUID.randomUUID();
        when(media.getMedia(image)).thenReturn(new MediaValidationDTO(image, outsider, "ACTIVE", "POST"));
        mvc.perform(patch("/api/posts/{id}", post.getId()).with(jwt().jwt(t -> t.subject(outsider.toString())).authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
            .contentType(MediaType.APPLICATION_JSON).content(body(Map.of("mediaIds", List.of(image)))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.mediaIds[0]").value(image.toString()));
        mvc.perform(patch("/api/posts/{id}", post.getId()).with(jwt().jwt(t -> t.subject(author.toString())))
            .contentType(MediaType.APPLICATION_JSON).content(body(Map.of("mediaIds", List.of(image), "content", "author edit"))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.mediaIds[0]").value(image.toString()));
        when(media.getMedia(image)).thenReturn(new MediaValidationDTO(image, outsider, "DELETED", "POST"));
        mvc.perform(patch("/api/posts/{id}", post.getId()).with(jwt().jwt(t -> t.subject(outsider.toString())).authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
            .contentType(MediaType.APPLICATION_JSON).content(body(Map.of("mediaIds", List.of(image), "content", "invalid"))))
            .andExpect(status().isForbidden());
        assertThat(posts.findById(post.getId()).orElseThrow().getContent()).isEqualTo("author edit");
    }

    @Test void noOpAndStaleVersionDoNotCreateRevision() throws Exception {
        mvc.perform(patch("/api/posts/{id}",post.getId()).with(jwt().jwt(t -> t.subject(author.toString()))).with(csrf())
            .contentType(MediaType.APPLICATION_JSON).content(body(Map.of("content","original"))))
            .andExpect(status().isOk());
        assertThat(revisions()).isZero();
        mvc.perform(patch("/api/posts/{id}",post.getId()).with(jwt().jwt(t -> t.subject(author.toString()))).with(csrf())
            .contentType(MediaType.APPLICATION_JSON).content(body(Map.of("version",-1,"content","lost edit"))))
            .andExpect(status().isConflict());
        assertThat(revisions()).isZero();
    }
    @Test void invalidMediaAndNonOwnerCannotChangeAnything() throws Exception {
        UUID image = UUID.randomUUID();
        when(media.getMedia(image)).thenReturn(new MediaValidationDTO(image, outsider, "ACTIVE", "POST"));
        mvc.perform(patch("/api/posts/{id}",post.getId()).with(jwt().jwt(t -> t.subject(author.toString()))).with(csrf())
            .contentType(MediaType.APPLICATION_JSON).content(body(Map.of("content","must rollback","mediaIds",List.of(image)))))
            .andExpect(status().isForbidden());
        mvc.perform(patch("/api/posts/{id}",post.getId()).with(jwt().jwt(t -> t.subject(outsider.toString()))).with(csrf())
            .contentType(MediaType.APPLICATION_JSON).content(body(Map.of("content","forbidden"))))
            .andExpect(status().isForbidden());
        assertThat(revisions()).isZero(); assertThat(posts.findById(post.getId()).orElseThrow().getContent()).isEqualTo("original");
    }
    @Test void tagAddsProfileMembershipWithoutOpeningPrivatePostAndAdminCanEdit() throws Exception {
        mvc.perform(patch("/api/posts/{id}",post.getId()).with(jwt().jwt(t -> t.subject(outsider.toString())).authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))).with(csrf())
            .contentType(MediaType.APPLICATION_JSON).content(body(Map.of("taggedUserIds",List.of(friend),"visibility","PRIVATE"))))
            .andExpect(status().isOk());
        mvc.perform(get("/api/posts/profile/{id}",friend).with(jwt().jwt(t -> t.subject(outsider.toString())))).andExpect(status().isOk()).andExpect(jsonPath("$.items").isEmpty());
        mvc.perform(get("/api/posts/profile/{id}",friend).with(jwt().jwt(t -> t.subject(author.toString()))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(1));
        mvc.perform(get("/api/posts/{id}/history",post.getId()).with(jwt().jwt(t -> t.subject(outsider.toString())))).andExpect(status().isForbidden());
        mvc.perform(patch("/api/posts/{id}",post.getId()).with(jwt().jwt(t -> t.subject(author.toString()))).with(csrf())
            .contentType(MediaType.APPLICATION_JSON).content(body(Map.of("visibility","PUBLIC"))))
            .andExpect(status().isOk());
        mvc.perform(get("/api/posts/{id}/history",post.getId()).with(jwt().jwt(t -> t.subject(outsider.toString())))).andExpect(status().isOk()).andExpect(jsonPath("$[1].snapshot.visibility").value("PRIVATE"));
        mvc.perform(get("/api/posts/profile/{id}",friend).with(jwt().jwt(t -> t.subject(outsider.toString())))).andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(1));
    }
    @Autowired org.springframework.security.oauth2.jwt.JwtDecoder decoder;
    @Test void invalidJwtDoesNotBecomeGuestAndInternalCannotWriteBusinessPost() throws Exception {
        when(decoder.decode("invalid")).thenThrow(new org.springframework.security.oauth2.jwt.BadJwtException("Invalid signature"));
        mvc.perform(get("/api/posts/{id}",post.getId()).header("Authorization","Bearer invalid")).andExpect(status().isUnauthorized());
        mvc.perform(patch("/api/posts/{id}",post.getId()).with(jwt().jwt(t -> t.subject(author.toString())).authorities(new SimpleGrantedAuthority("ROLE_INTERNAL")))
            .contentType(MediaType.APPLICATION_JSON).content(body(Map.of("content","service cannot edit"))))
            .andExpect(status().isForbidden());
    }
    @Test void profileHasIndependentKeysetAndTagRemovalUpdatesMembership() throws Exception {
        for (int i = 0; i < 5; i++) posts.saveAndFlush(new Post().authorId(author).content("timeline " + i)
            .visibility(PostVisibility.PUBLIC).status(PostStatus.ACTIVE).createdAt(published.plusSeconds(i + 1)));
        var taggedEntity = new Post().authorId(friend).content("tagged")
            .visibility(PostVisibility.PUBLIC).status(PostStatus.ACTIVE).createdAt(published.plusSeconds(10));
        taggedEntity.setTaggedUserIds(Set.of(author));
        var tagged = posts.saveAndFlush(taggedEntity);
        Set<UUID> seen = new HashSet<>(); String cursor = null; boolean more;
        do {
            var request = get("/api/posts/profile/{id}",author).with(jwt().jwt(t -> t.subject(author.toString()))).param("size","2");
            if (cursor != null) request.param("cursor",cursor);
            var response = mvc.perform(request).andExpect(status().isOk()).andReturn().getResponse();
            var page = json.readValue(response.getContentAsString(), com.minh.fakebook.post.service.PostBrowseService.ProfilePage.class);
            for (var item : page.items()) assertThat(seen.add(item.getId())).isTrue();
            more = page.hasMore(); cursor = page.nextCursor();
        } while (more);
        assertThat(seen).hasSize(7).contains(tagged.getId());
        mvc.perform(patch("/api/posts/{id}",tagged.getId()).with(jwt().jwt(t -> t.subject(friend.toString()))).with(csrf())
            .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("id",tagged.getId(),"version",tagged.getVersion(),"taggedUserIds",List.of()))))
            .andExpect(status().isOk());
        mvc.perform(get("/api/posts/profile/{id}",author).with(jwt().jwt(t -> t.subject(author.toString())))).andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(6));
    }

    @Test void deletedPostCannotBeEditedByOrdinaryOwner() throws Exception {
        post.setStatus(PostStatus.DELETED); posts.saveAndFlush(post);
        mvc.perform(patch("/api/posts/{id}",post.getId()).with(jwt().jwt(t -> t.subject(author.toString()))).with(csrf())
            .contentType(MediaType.APPLICATION_JSON).content(body(Map.of("content","must not edit"))))
            .andExpect(status().isForbidden());
        assertThat(revisions()).isZero();
    }
    @Test void candidateSourcesAreDisjointAndDoNotOpenStrangerPrivacy() throws Exception {
        UUID followed = UUID.randomUUID();
        var friendPublic = candidatePost(friend, PostVisibility.PUBLIC);
        var friendOnly = candidatePost(friend, PostVisibility.FRIENDS);
        candidatePost(friend, PostVisibility.PRIVATE);
        var followingPublic = candidatePost(followed, PostVisibility.PUBLIC);
        candidatePost(followed, PostVisibility.FRIENDS);
        var discovery = candidatePost(outsider, PostVisibility.PUBLIC);
        candidatePost(outsider, PostVisibility.FRIENDS);
        candidatePost(outsider, PostVisibility.PRIVATE);
        var deleted = candidatePost(outsider, PostVisibility.PUBLIC);
        deleted.setStatus(PostStatus.DELETED); posts.saveAndFlush(deleted);
        assertThat(candidates("FRIENDS",followed)).containsExactlyInAnyOrder(post.getId(),friendPublic.getId(),friendOnly.getId());
        assertThat(candidates("FOLLOWING",followed)).containsExactly(followingPublic.getId());
        assertThat(candidates("DISCOVERY",followed)).containsExactly(discovery.getId());
    }

    private Post candidatePost(UUID user, PostVisibility privacy) {
        return posts.saveAndFlush(new Post().authorId(user).content("candidate").visibility(privacy).status(PostStatus.ACTIVE).createdAt(published));
    }

    private List<UUID> candidates(String source, UUID followed) throws Exception {
        byte[] body = json.writeValueAsBytes(Map.of("viewerId",author,"friends",Set.of(friend),"following",Set.of(friend,followed),
            "source",source,"asOf",Instant.now(),"limit",100));
        var response = mvc.perform(post("/api/internal/feed-posts/candidates")
            .with(jwt().jwt(t -> t.subject(author.toString())).authorities(new SimpleGrantedAuthority("ROLE_INTERNAL")))
            .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk()).andReturn().getResponse();
        var page = json.readValue(response.getContentAsString(),com.minh.fakebook.post.service.PostBrowseService.CandidatePage.class);
        return page.items().stream().map(com.minh.fakebook.post.service.PostBrowseService.Candidate::postId).toList();
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({
        "PUBLIC,GUEST,401", "PUBLIC,OWNER,200", "PUBLIC,FRIEND,200", "PUBLIC,FOLLOWER,200", "PUBLIC,OUTSIDER,200", "PUBLIC,ADMIN,200",
        "FRIENDS,GUEST,401", "FRIENDS,OWNER,200", "FRIENDS,FRIEND,200", "FRIENDS,FOLLOWER,403", "FRIENDS,OUTSIDER,403", "FRIENDS,ADMIN,200",
        "PRIVATE,GUEST,401", "PRIVATE,OWNER,200", "PRIVATE,FRIEND,403", "PRIVATE,FOLLOWER,403", "PRIVATE,OUTSIDER,403", "PRIVATE,ADMIN,200",
        "DELETED,GUEST,401", "DELETED,OWNER,403", "DELETED,FRIEND,403", "DELETED,FOLLOWER,403", "DELETED,OUTSIDER,403", "DELETED,ADMIN,200"
    })
    void currentPostReadPermissionMatrix(String privacy, String viewerRole, int expected) throws Exception {
        UUID follower = UUID.randomUUID();
        when(users.areFriends(outsider,author)).thenReturn(false);
        when(users.areFriends(follower,author)).thenReturn(false);
        if (privacy.equals("DELETED")) post.setStatus(PostStatus.DELETED);
        else post.setVisibility(PostVisibility.valueOf(privacy));
        posts.saveAndFlush(post);
        var request = get("/api/posts/{id}",post.getId());
        if (!viewerRole.equals("GUEST")) {
            UUID viewer = switch (viewerRole) { case "OWNER" -> author; case "FRIEND" -> friend; case "FOLLOWER" -> follower; default -> outsider; };
            var authentication = jwt().jwt(token -> token.subject(viewer.toString()));
            if (viewerRole.equals("ADMIN")) authentication.authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
            request.with(authentication);
        }
        mvc.perform(request).andExpect(status().is(expected));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"DELETED,POST,false", "ACTIVE,AVATAR,false", "ACTIVE,POST,true"})
    void mediaStatusPurposeAndOwnerAreValidatedBeforeMutation(String mediaStatus, String purpose, boolean foreignOwner) throws Exception {
        UUID image = UUID.randomUUID();
        when(media.getMedia(image)).thenReturn(new MediaValidationDTO(image,foreignOwner ? outsider : author,mediaStatus,purpose));
        mvc.perform(patch("/api/posts/{id}",post.getId()).with(jwt().jwt(t -> t.subject(author.toString()))).with(csrf())
            .contentType(MediaType.APPLICATION_JSON).content(body(Map.of("content","must rollback","mediaIds",List.of(image)))))
            .andExpect(status().isForbidden());
        assertThat(revisions()).isZero();
        assertThat(posts.findById(post.getId()).orElseThrow().getContent()).isEqualTo("original");
    }
    @Test void savedPostsRequireUserJwtAtTheServiceBoundary() throws Exception {
        mvc.perform(get("/api/posts/saved")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/posts/saved").with(jwt().jwt(t -> t.subject(author.toString()))))
            .andExpect(status().isOk());
        mvc.perform(get("/api/posts/saved").with(jwt().jwt(t -> t.subject(author.toString())).authorities(new SimpleGrantedAuthority("ROLE_INTERNAL"))))
            .andExpect(status().isForbidden());
    }
}
