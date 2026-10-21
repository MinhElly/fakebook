package com.minh.fakebook.media.service;

import com.cloudinary.Cloudinary;
import com.minh.fakebook.media.domain.*;
import com.minh.fakebook.media.domain.enumeration.*;
import com.minh.fakebook.media.repository.*;
import com.minh.fakebook.media.service.dto.MediaDTO;
import com.minh.fakebook.media.service.mapper.MediaMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class DirectUploadService {
    private static final Set<String> FORMATS = Set.of("jpg", "jpeg", "png", "webp", "gif", "avif");
    public static final String VIDEO_TRANSFORMATION = "ac_aac,f_mp4,vc_h264";
    private static final Set<String> VIDEO_FORMATS = Set.of("mp4", "webm", "mov");
    private final Cloudinary cloudinary;
    private final MediaUploadSessionRepository sessions;
    private final MediaRepository media;
    private final MediaMapper mapper;
    private final PostMediaAccess postAccess;

    public DirectUploadService(Cloudinary cloudinary, MediaUploadSessionRepository sessions, MediaRepository media, MediaMapper mapper, PostMediaAccess postAccess) {
        this.cloudinary = cloudinary;
        this.sessions = sessions;
        this.media = media;
        this.mapper = mapper; this.postAccess = postAccess;
    }

    public record UploadAuthorization(UUID sessionId, String uploadUrl, String apiKey, Map<String, Object> parameters,
                                      String signature, Instant expiresAt) {}

    public UploadAuthorization sign(String fileName, String mimeType, long size) {
        return sign(fileName, mimeType, size, null, null);
    }
    public UploadAuthorization sign(String fileName, String mimeType, long size, UUID publicationId, UUID fileKey) {
        UUID owner = currentOwner();
        if ((publicationId == null) != (fileKey == null)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Publication and file key must be provided together");
        if (publicationId != null) requirePendingPublication(publicationId, owner);
        if (fileName == null || fileName.isBlank() || fileName.length() > 255 || size <= 0
            || !Set.of("image/jpeg", "image/png", "image/webp", "image/gif", "image/avif", "video/mp4", "video/webm", "video/quicktime").contains(mimeType == null ? "" : mimeType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Select a supported, nonempty image or video");
        }
        if (cloudinary.config.cloudName == null || cloudinary.config.cloudName.isBlank()
            || cloudinary.config.apiKey == null || cloudinary.config.apiKey.isBlank()
            || cloudinary.config.apiSecret == null || cloudinary.config.apiSecret.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Cloudinary is not configured");
        }
        MediaUploadSession session = new MediaUploadSession();
        session.id = publicationId == null ? UUID.randomUUID() : UUID.nameUUIDFromBytes((owner + ":" + publicationId + ":" + fileKey).getBytes(StandardCharsets.UTF_8));
        var existing = sessions.findById(session.id);
        if (existing.isPresent()) {
            session = existing.orElseThrow();
            if (!session.ownerId.equals(owner) || !Objects.equals(session.publicationId, publicationId)
                || !session.fileName.equals(fileName) || !session.resourceType.equals(mimeType.startsWith("video/") ? "video" : "image"))
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Upload key reused with a different file");
            if (!session.expiresAt.isAfter(Instant.now())) throw new ResponseStatusException(HttpStatus.GONE, "Upload expired");
            return authorization(session);
        }
        session.publicationId = publicationId;
        session.cleanupAfter = publicationId == null ? null : Instant.now().plusSeconds(60);
        session.ownerId = owner;
        session.publicId = "fakebook/users/" + owner + "/posts/" + session.id;
        session.fileName = fileName;
        session.resourceType = mimeType.startsWith("video/") ? "video" : "image";
        long timestamp = Instant.now().getEpochSecond();
        session.expiresAt = Instant.ofEpochSecond(timestamp + 3600);
        sessions.save(session);
        return authorization(session);
    }
    private UploadAuthorization authorization(MediaUploadSession session) {
        long timestamp = session.expiresAt.getEpochSecond() - 3600;
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("timestamp", timestamp);
        parameters.put("public_id", session.publicId);
        parameters.put("overwrite", false);
        parameters.put("type", "authenticated");
        parameters.put("allowed_formats", session.resourceType.equals("video") ? "mp4,webm,mov" : "jpg,jpeg,png,webp,gif,avif");
        if (session.resourceType.equals("video")) {
            parameters.put("eager", VIDEO_TRANSFORMATION + "|c_limit,so_0,w_640/f_jpg");
            parameters.put("eager_async", true);
        }
        String signature = cloudinary.apiSignRequest(parameters, cloudinary.config.apiSecret);
        return new UploadAuthorization(session.id,
            "https://api.cloudinary.com/v1_1/" + cloudinary.config.cloudName + "/" + session.resourceType + "/upload",
            cloudinary.config.apiKey, parameters, signature, session.expiresAt);
    }

    public MediaDTO complete(UUID sessionId) {
        UUID owner = currentOwner();
        MediaUploadSession session = sessions.findForCompletion(sessionId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Upload session not found"));
        if (!owner.equals(session.ownerId)) throw new AccessDeniedException("Upload belongs to another user");
        if (session.publicationId != null) requirePendingPublication(session.publicationId, owner);
        if (session.mediaId != null) {
            Media existing = media.findById(session.mediaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.GONE, "Media no longer exists"));
            if (existing.getStatus() != MediaStatus.ACTIVE) throw new ResponseStatusException(HttpStatus.GONE, "Media no longer active");
            return mapper.toDto(existing);
        }
        if (!session.expiresAt.isAfter(Instant.now())) throw new ResponseStatusException(HttpStatus.GONE, "Upload session expired");
        Map<?, ?> asset;
        try {
            asset = cloudinary.api().resource(session.publicId, Map.of("resource_type", session.resourceType, "type", "authenticated", "media_metadata", true));
        } catch (Exception error) {
            // Never trust the client's upload response as an alternative to provider verification.
            throw new StorageServiceUnavailableException("direct upload verification", error);
        }
        String format = Objects.toString(asset.get("format"), "");
        boolean video = session.resourceType.equals("video");
        if (!session.publicId.equals(asset.get("public_id")) || !session.resourceType.equals(asset.get("resource_type"))
            || !"authenticated".equals(asset.get("type")) || !(video ? VIDEO_FORMATS : FORMATS).contains(format)
            || !(asset.get("bytes") instanceof Number bytes) || bytes.longValue() <= 0
            || !(asset.get("width") instanceof Number width) || width.intValue() <= 0
            || !(asset.get("height") instanceof Number height) || height.intValue() <= 0) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Cloudinary asset is not supported media");
        }
        if (video && (!(asset.get("duration") instanceof Number duration) || duration.doubleValue() <= 0)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Video has no playable duration");
        }
        if (video && (!(asset.get("derived") instanceof List<?> derived) || derived.stream().noneMatch(value ->
            value instanceof Map<?, ?> entry && "mp4".equals(entry.get("format"))
                && VIDEO_TRANSFORMATION.equals(entry.get("transformation"))))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "MEDIA_PROCESSING");
        }
        Media result = new Media();
        result.setOwnerId(owner);
        result.setPublicationId(session.publicationId);
        result.setFileName(session.fileName);
        result.setMediaType(video ? MediaType.VIDEO : MediaType.IMAGE);
        result.setMimeType(video ? "video/mp4" : "image/" + (format.equals("jpg") ? "jpeg" : format));
        result.setFileSize(bytes.longValue());
        result.setStorageProvider(StorageProvider.CLOUDINARY);
        result.setStorageKey(session.publicId);
        // An unsigned authenticated URL is safe to expose. Only the file proxy signs delivery after authorization.
        result.setUrl(video ? cloudinary.url().secure(true).resourceType("video").type("authenticated")
            .transformation(new com.cloudinary.Transformation().rawTransformation(VIDEO_TRANSFORMATION)).format("mp4").generate(session.publicId)
            : cloudinary.url().secure(true).resourceType("image").type("authenticated").format(format).generate(session.publicId));
        result.setPurpose(MediaPurpose.POST);
        result.setStatus(MediaStatus.ACTIVE);
        result.setCreatedAt(Instant.now());
        result = media.saveAndFlush(result);
        session.mediaId = result.getId();
        return mapper.toDto(result);
    }

    public String storageReadUrl(MediaDTO dto) {
        if (dto.getStorageKey() != null && dto.getUrl() != null && dto.getUrl().contains("/video/authenticated/")) {
            return cloudinary.url().secure(true).signed(true).resourceType("video").type("authenticated")
                .transformation(new com.cloudinary.Transformation().rawTransformation(VIDEO_TRANSFORMATION)).format("mp4").generate(dto.getStorageKey());
        }
        if (dto.getStorageKey() != null && dto.getUrl() != null && dto.getUrl().contains("/image/authenticated/")) {
            return cloudinary.url().secure(true).signed(true).resourceType("image").type("authenticated")
                .generate(dto.getStorageKey());
        }
        return dto.getUrl();
    }

    public String posterReadUrl(MediaDTO dto) {
        return cloudinary.url().secure(true).signed(true).resourceType("video")
            .type(dto.getUrl() != null && dto.getUrl().contains("/authenticated/") ? "authenticated" : "upload")
            .transformation(new com.cloudinary.Transformation().rawTransformation("c_limit,so_0,w_640/f_jpg"))
            .format("jpg").generate(dto.getStorageKey());
    }

    /** Leave a full day after expiration before deleting an abandoned authenticated upload. */
    public void cleanExpiredSession(UUID id) {
        var session = sessions.findForCompletion(id).orElse(null);
        if (session == null || session.mediaId != null || !session.expiresAt.isBefore(Instant.now().minusSeconds(86400))) return;
        try {
            Map<?, ?> result = cloudinary.uploader().destroy(session.publicId,
                Map.of("resource_type", session.resourceType, "type", "authenticated", "invalidate", true));
            if (!Set.of("ok", "not found").contains(Objects.toString(result.get("result"), ""))) {
                throw new IllegalStateException("Cloudinary did not confirm cleanup");
            }
        } catch (Exception error) {
            throw new StorageServiceUnavailableException("abandoned direct upload cleanup", error);
        }
        sessions.delete(session);
    }

    private void requirePendingPublication(UUID id, UUID owner) {
        var publication = postAccess.publication(id);
        if (!owner.equals(publication.ownerId())) throw new AccessDeniedException("Publication belongs to another user");
        if (!"PENDING".equals(publication.status()) || !publication.expiresAt().isAfter(Instant.now()))
            throw new ResponseStatusException(HttpStatus.GONE, "Publication cancelled, published or expired");
    }
    /** Durable, repeatable compensation. Keep retrying through the upload signature's lifetime. */
    public void compensatePublication(UUID id) {
        var session = sessions.findForCompletion(id).orElse(null);
        if (session == null || session.publicationId == null) return;
        // Move the cursor before remote work; transaction rollback preserves retry on failure.
        session.cleanupAfter = Instant.now().plusSeconds(60);
        var publication = postAccess.publication(session.publicationId);
        if (!session.ownerId.equals(publication.ownerId())) throw new IllegalStateException("Publication owner mismatch");
        if ("PENDING".equals(publication.status())) return;
        if (!Set.of("CANCELLED", "PUBLISHED").contains(publication.status())) throw new IllegalStateException("Unknown publication decision");
        if (session.mediaId != null && postAccess.isAttached(session.mediaId)) {
            if (!"PUBLISHED".equals(publication.status())) throw new IllegalStateException("Cancelled publication unexpectedly has an attached media");
            session.cleanupAfter = null;
            return;
        }
        try {
            var result = cloudinary.uploader().destroy(session.publicId, Map.of("resource_type", session.resourceType, "type", "authenticated", "invalidate", true));
            if (!Set.of("ok", "not found").contains(Objects.toString(result.get("result"), ""))) throw new IllegalStateException("Deletion was not confirmed");
        } catch (Exception error) { throw new StorageServiceUnavailableException("publication compensation", error); }
        if (session.mediaId != null) media.findById(session.mediaId).ifPresent(value -> value.setStatus(MediaStatus.DELETED));
        // A signed upload can arrive after cancellation; repeated destroy catches these late arrivals.
        Instant now = Instant.now();
        if (session.expiresAt.plusSeconds(86400).isBefore(now)) session.cleanupAfter = null;
        else if (session.expiresAt.plusSeconds(300).isAfter(now)) session.cleanupAfter = session.expiresAt.plusSeconds(300);
        else session.cleanupAfter = session.expiresAt.plusSeconds(86401);
    }

    public void deferCompensation(UUID id) {
        sessions.findForCompletion(id).ifPresent(session -> session.cleanupAfter = Instant.now().plusSeconds(60));
    }

    private UUID currentOwner() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken
            || auth.getAuthorities().stream().anyMatch(a -> Set.of("ROLE_ADMIN", "ROLE_INTERNAL").contains(a.getAuthority()))) {
            throw new AccessDeniedException("A community account is required");
        }
        String subject = auth instanceof JwtAuthenticationToken jwt ? jwt.getToken().getSubject() : auth.getName();
        try { return UUID.fromString(subject); }
        catch (IllegalArgumentException error) { return UUID.nameUUIDFromBytes(subject.getBytes(StandardCharsets.UTF_8)); }
    }
}
