package com.minh.fakebook.media.web.rest;

import com.minh.fakebook.media.service.DirectUploadService;
import com.minh.fakebook.media.service.dto.MediaDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/media/uploads")
public class DirectUploadResource {
    private final DirectUploadService service;
    public DirectUploadResource(DirectUploadService service) { this.service = service; }

    public record SignRequest(@NotBlank @Size(max = 255) String fileName, @NotBlank String mimeType, @Positive long size, UUID publicationId, UUID fileKey) {}
    public record CompleteRequest(@NotNull UUID sessionId) {}

    @PostMapping("/sign")
    public ResponseEntity<DirectUploadService.UploadAuthorization> sign(@Valid @RequestBody SignRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
            .body(service.sign(request.fileName(), request.mimeType(), request.size(), request.publicationId(), request.fileKey()));
    }

    @PostMapping("/complete")
    public MediaDTO complete(@Valid @RequestBody CompleteRequest request) {
        return service.complete(request.sessionId());
    }
}
