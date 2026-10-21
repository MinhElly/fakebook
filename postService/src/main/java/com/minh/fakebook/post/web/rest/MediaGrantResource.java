package com.minh.fakebook.post.web.rest;
import com.minh.fakebook.post.administration.*;

import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import com.minh.fakebook.post.service.PostService;
@RestController
public class MediaGrantResource {
    private final JdbcTemplate jdbc;
    private final PostService posts;
    public MediaGrantResource(JdbcTemplate jdbc,PostService posts) {this.jdbc=jdbc;this.posts=posts;}
    @PostMapping("/api/posts/media/access-batch")
    public Map<UUID, Map<String, Boolean>> batch(@RequestBody List<UUID> ids) {
        if (ids.size() > 50 || ids.stream().anyMatch(Objects::isNull)) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST);
        var result = new LinkedHashMap<UUID, Map<String, Boolean>>();
        if (ids.isEmpty()) return result;
        var rows = jdbc.queryForList("select media_id,post_id from post_media where media_id in (" + String.join(",",Collections.nCopies(ids.size(),"?")) + ")", ids.stream().map(UUID::toString).toArray());
        var readable = new HashMap<String, Boolean>();
        for (var row : rows) {
            String parent = row.get("post_id").toString();
            if (!readable.containsKey(parent)) {
                try { readable.put(parent, posts.findOne(UUID.fromString(parent)).isPresent()); }
                catch (org.springframework.security.access.AccessDeniedException denied) { readable.put(parent, false); }
            }
        }
        for (UUID id : ids) {
            var parents = rows.stream().filter(row -> row.get("media_id").toString().equals(id.toString())).toList();
            result.put(id, Map.of("attached", !parents.isEmpty(), "readable", parents.stream().anyMatch(row -> Boolean.TRUE.equals(readable.get(row.get("post_id").toString())))));
        }
        return result;
    }
    @GetMapping("/api/internal/media/{id}/attached")
    public boolean attached(@PathVariable UUID id) {
        return !jdbc.queryForList("select post_id from post_media where media_id=?", String.class, id.toString()).isEmpty();
    }
    @GetMapping("/api/posts/media/{id}/attachment-access")
    public Map<String,Boolean> access(@PathVariable UUID id) {
        var parents=jdbc.queryForList("select post_id from post_media where media_id=?",String.class,id.toString());
        boolean readable=false;
        for(String parent:parents) {
            try {if(posts.findOne(UUID.fromString(parent)).isPresent()) {readable=true;break;}}
            catch(org.springframework.security.access.AccessDeniedException denied) { /* Another parent can grant access. */ }
        }
        return Map.of("attached",!parents.isEmpty(),"readable",readable);
    }
}
