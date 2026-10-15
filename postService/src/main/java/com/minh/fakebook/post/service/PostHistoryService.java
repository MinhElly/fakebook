package com.minh.fakebook.post.service;

import com.minh.fakebook.post.service.dto.PostDTO;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/** Text and change metadata only: historical asset bytes are not retained. */
@Service
@Transactional
public class PostHistoryService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    public PostHistoryService(JdbcTemplate jdbc, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    public record Revision(long revision, UUID editorId, Instant editedAt, List<String> changedFields, PostDTO snapshot) {}

    public void baseline(PostDTO post) {
        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM post_revision WHERE post_id = ?", Long.class, post.getId().toString());
        if (count == 0) {
            try {
                record(post, post.getAuthorId(), List.of("BASELINE"), post.getUpdatedAt() == null ? post.getCreatedAt() : post.getUpdatedAt());
            } catch (org.springframework.dao.DuplicateKeyException concurrentBaseline) {
                throw new org.springframework.dao.OptimisticLockingFailureException("Post changed; reload before editing", concurrentBaseline);
            }
        }
    }

    public void record(PostDTO post, UUID editor, List<String> fields, Instant time) {
        jdbc.update("INSERT INTO post_revision (post_id, revision, editor_id, edited_at, changed_fields, snapshot) VALUES (?, ?, ?, ?, ?, ?)",
            post.getId().toString(), post.getVersion() == null ? 0 : post.getVersion(), editor.toString(), time.toString(),
            String.join(",", fields), mapper.writeValueAsString(post));
    }

    @Transactional(readOnly = true)
    public List<Revision> list(UUID id, Long before, int size) {
        return jdbc.query("SELECT * FROM post_revision WHERE post_id = ? AND revision < ? ORDER BY revision DESC LIMIT ?",
            (rs, row) -> new Revision(rs.getLong("revision"), UUID.fromString(rs.getString("editor_id")),
                Instant.parse(rs.getString("edited_at")), List.of(rs.getString("changed_fields").split(",")),
                mapper.readValue(rs.getString("snapshot"), PostDTO.class)),
            id.toString(), before == null ? Long.MAX_VALUE : before, Math.clamp(size, 1, 50));
    }

    public void delete(UUID id) { jdbc.update("DELETE FROM post_revision WHERE post_id = ?", id.toString()); }
}
