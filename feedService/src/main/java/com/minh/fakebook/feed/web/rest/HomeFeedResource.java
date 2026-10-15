package com.minh.fakebook.feed.web.rest;
import com.minh.fakebook.feed.service.HomeFeedService;
import jakarta.servlet.http.*;
import java.util.UUID;
import org.springframework.http.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/feed")
public class HomeFeedResource {
    private final HomeFeedService feeds;
    public HomeFeedResource(HomeFeedService feeds) { this.feeds = feeds; }
    @GetMapping({"/home", "/friends"})
    public ResponseEntity<HomeFeedService.Page> read(@RequestParam(required = false) String cursor,
            HttpServletRequest request) {
        String mode = request.getRequestURI().endsWith("/friends") ? "friends" : "home";
        var auth = SecurityContextHolder.getContext().getAuthentication();
        UUID viewer = auth instanceof JwtAuthenticationToken jwt ? UUID.fromString(jwt.getToken().getSubject()) : null;
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(feeds.get(viewer, mode, cursor));
    }
}
