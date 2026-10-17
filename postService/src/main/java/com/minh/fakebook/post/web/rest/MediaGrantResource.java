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
