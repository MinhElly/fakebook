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
            @CookieValue(name = "fakebook_guest", required = false) String cookie, HttpServletRequest request, HttpServletResponse response) {
        String mode = request.getRequestURI().endsWith("/friends") ? "friends" : "home";
        var auth = SecurityContextHolder.getContext().getAuthentication();
        UUID viewer = auth instanceof JwtAuthenticationToken jwt ? UUID.fromString(jwt.getToken().getSubject()) : null;
        UUID guest = null;
        if (viewer == null) {
            UUID proposed = null;
            try { proposed = cookie == null ? null : UUID.fromString(cookie); } catch (IllegalArgumentException ignored) { }
            guest = feeds.guest(proposed);
            response.addHeader("Set-Cookie", ResponseCookie.from("fakebook_guest", guest.toString()).httpOnly(true).secure(request.isSecure()).sameSite("Lax").path("/").build().toString());
        }
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(feeds.get(viewer, guest, mode, cursor));
    }
}
