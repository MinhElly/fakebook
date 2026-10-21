package com.minh.fakebook.post.web.rest;
import com.minh.fakebook.post.service.PostPublicationService;
import java.util.*;
import org.springframework.web.bind.annotation.*;
@RestController
public class PostPublicationResource {
    private final PostPublicationService service;
    public PostPublicationResource(PostPublicationService service) { this.service = service; }
    public record Completion(List<UUID> mediaIds) {}
    @PutMapping("/api/posts/publications/{id}")
    public PostPublicationService.View begin(@PathVariable UUID id, @RequestBody PostPublicationService.Draft draft) { return service.begin(id, draft); }
    @GetMapping("/api/posts/publications/{id}")
    public PostPublicationService.View status(@PathVariable UUID id) { return service.status(id); }
    @PostMapping("/api/posts/publications/{id}/publish")
    public PostPublicationService.View finish(@PathVariable UUID id, @RequestBody Completion request) {
        try { return service.finish(id, request.mediaIds()); }
        catch (RuntimeException error) {
            // finish's transaction has rolled back before compensating in a new transaction.
            // If commit actually succeeded, cancel sees PUBLISHED and never undoes the post.
            try { service.cancel(id); } catch (RuntimeException ignored) { /* Expiry will retry independently. */ }
            throw error;
        }
    }
    @DeleteMapping("/api/posts/publications/{id}")
    public PostPublicationService.View cancel(@PathVariable UUID id) { return service.cancel(id); }
    @GetMapping("/api/internal/publications/{id}")
    public PostPublicationService.View internalStatus(@PathVariable UUID id) { return service.internalStatus(id); }
}
