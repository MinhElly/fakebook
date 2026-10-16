package com.minh.fakebook.comment.service;

import com.minh.fakebook.comment.domain.Comment;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Immutable content snapshots; participates in the comment mutation transaction. */
@Service
@Transactional
public class CommentHistoryService {
    private final JdbcTemplate jdbc;

    public CommentHistoryService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record Revision(long revision, String content, UUID editorId, Instant editedAt) {}

    public void record(Comment comment, UUID editor, Instant time) {
        jdbc.update(
            "INSERT INTO comment_revision (comment_id, revision, content, editor_id, edited_at) VALUES (?, ?, ?, ?, ?)",
            comment.getId().toString(), comment.getEditCount(), comment.getContent(), editor.toString(),
            time == null ? null : Timestamp.from(time)
        );
    }

    @Transactional(readOnly = true)
    public List<Revision> list(UUID id, Long before, int size) {
        return jdbc.query(
            "SELECT revision, content, editor_id, edited_at FROM comment_revision WHERE comment_id = ? AND revision < ? ORDER BY revision DESC LIMIT ?",
            (rs, row) -> {
                Timestamp time = rs.getTimestamp("edited_at");
                return new Revision(rs.getLong("revision"), rs.getString("content"), UUID.fromString(rs.getString("editor_id")),
                    time == null ? null : time.toInstant());
            },
            id.toString(), before == null ? Long.MAX_VALUE : before, Math.max(1, Math.min(size, 50))
        );
    }
}
