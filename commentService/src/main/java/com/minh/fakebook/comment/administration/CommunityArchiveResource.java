package com.minh.fakebook.comment.administration;

import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/internal/accounts")
@PreAuthorize("hasAuthority('ROLE_INTERNAL')")
@Transactional
public class CommunityArchiveResource {
    private final JdbcTemplate jdbc;
    public CommunityArchiveResource(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    public record ArchiveRequest(@jakarta.validation.constraints.NotNull Instant before) {}
    @PostMapping("/{subject}/archive")
    public void archive(@PathVariable UUID subject,@jakarta.validation.Valid @RequestBody ArchiveRequest request) {
        jdbc.update("update comment_reaction set community_archived=true where user_id=? and created_at<=?",subject.toString(),java.sql.Timestamp.from(request.before()));
        jdbc.update("update comments set community_archived=true where author_id=? and created_at<=?",subject.toString(),java.sql.Timestamp.from(request.before()));
        for (var post:jdbc.queryForList("select distinct post_id from comments where author_id=?",String.class,subject.toString())) CommentModeration.recompute(jdbc,post);
    }
}
