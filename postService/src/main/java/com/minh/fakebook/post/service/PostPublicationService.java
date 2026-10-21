package com.minh.fakebook.post.service;

import com.minh.fakebook.post.domain.enumeration.PostVisibility;
import com.minh.fakebook.post.service.dto.PostDTO;
import java.time.Instant;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

/** A durable publication decision, committed atomically with the Post and its outbox. */
@Service
public class PostPublicationService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final PostService posts;
    public PostPublicationService(JdbcTemplate jdbc, ObjectMapper mapper, PostService posts) {
        this.jdbc = jdbc; this.mapper = mapper; this.posts = posts;
    }
    public record Draft(String content, PostVisibility visibility, List<UUID> taggedUserIds, UUID targetPostId, Long version) {}
    public record View(UUID id, UUID ownerId, String status, Instant expiresAt, PostDTO post) {}
    private record Row(UUID id, UUID ownerId, String status, Instant expiresAt, String draft, String attachments, String result) {}
    private UUID owner() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof JwtAuthenticationToken jwt) || auth.getAuthorities().stream().anyMatch(a -> Set.of("ROLE_ADMIN", "ROLE_INTERNAL").contains(a.getAuthority())))
            throw new AccessDeniedException("A community account is required");
        return UUID.fromString(jwt.getToken().getSubject());
    }
    private Row row(UUID id, boolean lock) {
        return jdbc.query("SELECT * FROM post_publication WHERE id = ?" + (lock ? " FOR UPDATE" : ""),
            (rs, n) -> new Row(UUID.fromString(rs.getString("id")), UUID.fromString(rs.getString("owner_id")), rs.getString("status"),
                Instant.ofEpochSecond(rs.getLong("expires_at")), rs.getString("draft_json"), rs.getString("attachments_json"), rs.getString("result_json")), id.toString())
            .stream().findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Publication not found"));
    }
    private Row owned(UUID id, boolean lock) {
        UUID caller = owner(); Row row = row(id, lock);
        if (!row.ownerId.equals(caller)) throw new AccessDeniedException("Publication belongs to another user");
        return row;
    }
    private View view(Row row) {
        return new View(row.id, row.ownerId, row.status, row.expiresAt, row.result == null ? null : mapper.readValue(row.result, PostDTO.class));
    }
    @Transactional
    public View begin(UUID id, Draft draft) {
        UUID caller = owner();
        if (draft == null || draft.visibility == null || draft.content == null || draft.content.length() > 100000
            || draft.taggedUserIds != null && draft.taggedUserIds.size() > 100)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid publication draft");
        if (draft.targetPostId != null) {
            var post = posts.findOne(draft.targetPostId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            if (!caller.equals(post.getAuthorId())) throw new AccessDeniedException("Only the author can edit this post");
        }
        String json = mapper.writeValueAsString(draft);
        jdbc.update("INSERT INTO post_publication (id, owner_id, status, expires_at, draft_json) VALUES (?, ?, 'PENDING', ?, ?) ON DUPLICATE KEY UPDATE id = id",
            id.toString(), caller.toString(), Instant.now().getEpochSecond() + 3600, json);
        Row row = owned(id, true);
        if (!row.draft.equals(json)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Idempotency key was used with a different draft");
        return view(row);
    }
    @Transactional(readOnly = true)
    public View status(UUID id) { return view(owned(id, false)); }
    @Transactional(readOnly = true)
    public View internalStatus(UUID id) {
        Row row = row(id, false);
        return new View(row.id, row.ownerId, row.status, row.expiresAt, null);
    }
    @Transactional
    public View finish(UUID id, List<UUID> mediaIds) {
        Row row = owned(id, true);
        if (mediaIds == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "mediaIds is required");
        PostAttachmentRules.validate(mediaIds);
        String attachments = mapper.writeValueAsString(mediaIds);
        if (row.status.equals("PUBLISHED")) {
            if (!Objects.equals(row.attachments, attachments)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Publication already committed with different media");
            return view(row);
        }
        if (!row.status.equals("PENDING") || !row.expiresAt.isAfter(Instant.now())) throw new ResponseStatusException(HttpStatus.GONE, "Publication cancelled or expired");
        Draft draft = mapper.readValue(row.draft, Draft.class);
        if (draft.content.isBlank() && mediaIds.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Post is empty");
        PostDTO result = posts.publish(id, draft, mediaIds);
        jdbc.update("UPDATE post_publication SET status='PUBLISHED', attachments_json=?, result_json=? WHERE id=?",
            attachments, mapper.writeValueAsString(result), id.toString());
        return view(row(id, false));
    }
    @Transactional
    public View cancel(UUID id) {
        Row row = owned(id, true);
        if (row.status.equals("PENDING")) jdbc.update("UPDATE post_publication SET status='CANCELLED' WHERE id=?", id.toString());
        return view(row(id, false));
    }
    @Transactional
    public void expire() {
        // The update takes the same row lock as finish: cancellation cannot win halfway through publication.
        jdbc.update("UPDATE post_publication SET status='CANCELLED' WHERE status='PENDING' AND expires_at < ?", Instant.now().getEpochSecond());
    }
}
