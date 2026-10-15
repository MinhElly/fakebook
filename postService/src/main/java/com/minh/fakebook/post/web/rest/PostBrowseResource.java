package com.minh.fakebook.post.web.rest;
import com.minh.fakebook.post.service.*;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts")
public class PostBrowseResource {
    private final PostBrowseService browse;
    public PostBrowseResource(PostBrowseService browse) { this.browse = browse; }
    @GetMapping("/profile/{userId}")
    public PostBrowseService.ProfilePage profile(@PathVariable UUID userId, @RequestParam(required = false) String cursor, @RequestParam(defaultValue = "11") int size) {
        if (size < 1 || size > 50) throw new IllegalArgumentException("Size must be 1..50");
        return browse.profile(userId, cursor, size);
    }
    @GetMapping("/{id}/history")
    public java.util.List<PostHistoryService.Revision> history(@PathVariable UUID id, @RequestParam(required = false) Long before, @RequestParam(defaultValue = "20") int size) {
        return browse.history(id, before, size);
    }
    @GetMapping("/media/{id}/access")
    public boolean mediaAccess(@PathVariable UUID id) { return browse.mediaReadable(id); }
}
