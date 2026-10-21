package com.minh.fakebook.media.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.cloudinary.Cloudinary;
import com.minh.fakebook.media.domain.*;
import com.minh.fakebook.media.domain.enumeration.*;
import com.minh.fakebook.media.repository.*;
import com.minh.fakebook.media.service.mapper.MediaMapper;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

class DirectUploadServiceTest {
    private final UUID owner = UUID.randomUUID();
    private final PostMediaAccess postAccess = mock(PostMediaAccess.class);
    private final Cloudinary cloudinary = spy(new Cloudinary(Map.of("cloud_name", "test-cloud", "api_key", "test-key", "api_secret", "test-secret")));
    private final com.cloudinary.Api provider = mock(com.cloudinary.Api.class);
    private final MediaUploadSessionRepository sessions = mock(MediaUploadSessionRepository.class);
    private final MediaRepository media = mock(MediaRepository.class);
    private final MediaMapper mapper = mock(MediaMapper.class);
    private final DirectUploadService service = new DirectUploadService(cloudinary, sessions, media, mapper, postAccess);

    @BeforeEach void setup() {
        authenticate(owner, "ROLE_USER");
        doReturn(provider).when(cloudinary).api();
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    private void authenticate(UUID id, String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
            id.toString(), "", List.of(new SimpleGrantedAuthority(role))));
    }
    private MediaUploadSession session() {
        var session = new MediaUploadSession();
        session.id = UUID.randomUUID(); session.ownerId = owner;
        session.publicId = "fakebook/users/" + owner + "/posts/" + session.id;
        session.fileName = "photo.png"; session.expiresAt = Instant.now().plusSeconds(3600);
        when(sessions.findForCompletion(session.id)).thenReturn(Optional.of(session));
        return session;
    }
    private com.cloudinary.api.ApiResponse asset(MediaUploadSession session) {
        var values = new HashMap<>(Map.of("public_id", session.publicId, "resource_type", "image", "type", "authenticated",
            "format", "png", "bytes", 1234L, "width", 50, "height", 50));
        return mock(com.cloudinary.api.ApiResponse.class, org.mockito.AdditionalAnswers.delegatesTo(values));
    }

    @Test void signatureBindsPrivateAssetAndDisablesOverwrite() {
        var result = service.sign("photo.png", "image/png", 100);
        assertThat(result.parameters()).containsEntry("type", "authenticated").containsEntry("overwrite", false);
        assertThat(result.parameters().get("public_id").toString()).contains(owner.toString());
        assertThat(result.signature()).isEqualTo(cloudinary.apiSignRequest(result.parameters(), "test-secret"));
        assertThat(result.toString()).doesNotContain("test-secret");
        verify(sessions).save(any(MediaUploadSession.class));
        verifyNoInteractions(provider);
    }
    @Test void rejectsInvalidInputAndAdministrativeAccounts() {
        assertThatThrownBy(() -> service.sign("document.pdf", "application/pdf", 123)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.sign("empty.png", "image/png", 0)).isInstanceOf(ResponseStatusException.class);
        authenticate(owner, "ROLE_ADMIN");
        assertThatThrownBy(() -> service.sign("photo.png", "image/png", 100)).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(sessions);
    }
    @Test void completionChecksOwnershipAndExpiryBeforeContactingCloudinary() {
        var session = session();
        authenticate(UUID.randomUUID(), "ROLE_USER");
        assertThatThrownBy(() -> service.complete(session.id)).isInstanceOf(AccessDeniedException.class);
        authenticate(owner, "ROLE_USER");
        session.expiresAt = Instant.now().minusSeconds(1);
        assertThatThrownBy(() -> service.complete(session.id)).isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(provider, media);
    }
    @Test void completionPersistsProviderMetadataAndRetryDoesNotCreateAnotherMedia() throws Exception {
        var session = session();
        when(provider.resource(eq(session.publicId), anyMap())).thenReturn(asset(session));
        when(media.saveAndFlush(any())).thenAnswer(invocation -> {
            Media value = invocation.getArgument(0); value.setId(UUID.randomUUID()); return value;
        });
        service.complete(session.id);
        var captured = org.mockito.ArgumentCaptor.forClass(Media.class);
        verify(media).saveAndFlush(captured.capture());
        Media saved = captured.getValue();
        assertThat(saved.getFileSize()).isEqualTo(1234);
        assertThat(saved.getMimeType()).isEqualTo("image/png");
        assertThat(saved.getPurpose()).isEqualTo(MediaPurpose.POST);
        assertThat(saved.getUrl()).contains("/image/authenticated/").doesNotContain("/s--");
        when(media.findById(saved.getId())).thenReturn(Optional.of(saved));
        service.complete(session.id);
        verify(provider, times(1)).resource(eq(session.publicId), anyMap());
        verify(media, times(1)).saveAndFlush(any());
    }
    @Test void rejectsWrongResourceTypeAndFailsClosedWhenProviderIsUnavailable() throws Exception {
        var session = session();
        var invalid = asset(session); invalid.put("resource_type", "video");
        when(provider.resource(eq(session.publicId), anyMap())).thenReturn(invalid);
        assertThatThrownBy(() -> service.complete(session.id)).isInstanceOf(ResponseStatusException.class);
        when(provider.resource(eq(session.publicId), anyMap())).thenThrow(new RuntimeException("offline"));
        assertThatThrownBy(() -> service.complete(session.id)).isInstanceOf(StorageServiceUnavailableException.class);
        verifyNoInteractions(media);
    }
    @Test void protectedDeliveryIsSignedOnlyForBackendRead() {
        var dto = new com.minh.fakebook.media.service.dto.MediaDTO();
        dto.setStorageKey("fakebook/test");
        dto.setUrl("https://res.cloudinary.com/test-cloud/image/authenticated/fakebook/test.png");
        assertThat(service.storageReadUrl(dto)).contains("/image/authenticated/s--");
        dto.setUrl("https://res.cloudinary.com/test-cloud/image/upload/legacy.png");
        assertThat(service.storageReadUrl(dto)).isEqualTo(dto.getUrl());
    }
    @Test void videoSignatureRequestsAsyncPlayableMp4() {
        var result = service.sign("movie.mov", "video/quicktime", 200_000_000);
        assertThat(result.uploadUrl()).endsWith("/video/upload");
        assertThat(result.parameters()).containsEntry("eager_async", true);
        assertThat(result.parameters().get("eager").toString()).startsWith(DirectUploadService.VIDEO_TRANSFORMATION + "|");
    }
    @Test void videoCannotBeAttachedUntilVerifiedTranscodeExists() throws Exception {
        var session = session(); session.resourceType = "video";
        var asset = asset(session); asset.put("resource_type", "video"); asset.put("format", "mov"); asset.put("duration", 2.5);
        when(provider.resource(eq(session.publicId), anyMap())).thenAnswer(invocation -> {
            Map<?, ?> options = invocation.getArgument(1);
            assertThat(options.get("media_metadata")).isEqualTo(true);
            return asset;
        });
        assertThatThrownBy(() -> service.complete(session.id)).isInstanceOf(ResponseStatusException.class).hasMessageContaining("MEDIA_PROCESSING");
        verifyNoInteractions(media);
        asset.put("derived", List.of(Map.of("format", "mp4", "transformation", DirectUploadService.VIDEO_TRANSFORMATION)));
        when(media.saveAndFlush(any())).thenAnswer(invocation -> { Media value = invocation.getArgument(0); value.setId(UUID.randomUUID()); return value; });
        service.complete(session.id);
        var captor = org.mockito.ArgumentCaptor.forClass(Media.class); verify(media).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getMediaType()).isEqualTo(com.minh.fakebook.media.domain.enumeration.MediaType.VIDEO);
        assertThat(captor.getValue().getMimeType()).isEqualTo("video/mp4");
        assertThat(captor.getValue().getUrl()).contains("/video/authenticated/ac_aac,f_mp4,vc_h264/");
    }
    @Test void cleanupNeverDeletesCompletedOrRecentlyExpiredSessions() {
        var session = session();
        session.mediaId = UUID.randomUUID(); session.expiresAt = Instant.now().minusSeconds(172800);
        service.cleanExpiredSession(session.id);
        session.mediaId = null; session.expiresAt = Instant.now().minusSeconds(30);
        service.cleanExpiredSession(session.id);
        verify(cloudinary, never()).uploader();
        verify(sessions, never()).delete(any());
    }
    @Test void cleanupRetainsSessionForRetryOnStorageFailure() throws Exception {
        var session = session(); session.expiresAt = Instant.now().minusSeconds(172800);
        var uploader = mock(com.cloudinary.Uploader.class);
        doReturn(uploader).when(cloudinary).uploader();
        when(uploader.destroy(eq(session.publicId), anyMap())).thenThrow(new java.io.IOException("offline"));
        assertThatThrownBy(() -> service.cleanExpiredSession(session.id)).isInstanceOf(StorageServiceUnavailableException.class);
        verify(sessions, never()).delete(any());
        when(uploader.destroy(eq(session.publicId), anyMap())).thenReturn(Map.of("result", "ok"));
        service.cleanExpiredSession(session.id);
        verify(sessions).delete(session);
    }
    @Test void publicationUploadKeyReplaysSameSignatureAndRejectsCancelledOwnerMismatch() {
        UUID publication=UUID.randomUUID(), key=UUID.randomUUID();
        when(postAccess.publication(publication)).thenReturn(new PostMediaAccess.Publication(publication,owner,"PENDING",Instant.now().plusSeconds(3600)));
        var first=service.sign("photo.png","image/png",100,publication,key);
        var slot=org.mockito.ArgumentCaptor.forClass(MediaUploadSession.class);verify(sessions).save(slot.capture());
        when(sessions.findById(first.sessionId())).thenReturn(Optional.of(slot.getValue()));
        var replay=service.sign("photo.png","image/png",100,publication,key);
        assertThat(replay).isEqualTo(first);verify(sessions,times(1)).save(any());
        when(postAccess.publication(publication)).thenReturn(new PostMediaAccess.Publication(publication,owner,"CANCELLED",Instant.now().plusSeconds(3600)));
        assertThatThrownBy(()->service.sign("photo.png","image/png",100,publication,key)).isInstanceOf(ResponseStatusException.class);
        when(postAccess.publication(publication)).thenReturn(new PostMediaAccess.Publication(publication,UUID.randomUUID(),"PENDING",Instant.now().plusSeconds(3600)));
        assertThatThrownBy(()->service.sign("photo.png","image/png",100,publication,key)).isInstanceOf(AccessDeniedException.class);
    }
    @Test void compensationRetriesFailedDestroyAndCatchesLateUploadAfterCancellation() throws Exception {
        var session=session();session.publicationId=UUID.randomUUID();
        when(postAccess.publication(session.publicationId)).thenReturn(new PostMediaAccess.Publication(session.publicationId,owner,"CANCELLED",Instant.now()));
        var uploader=mock(com.cloudinary.Uploader.class);doReturn(uploader).when(cloudinary).uploader();
        when(uploader.destroy(eq(session.publicId),anyMap())).thenThrow(new java.io.IOException("offline"));
        assertThatThrownBy(()->service.compensatePublication(session.id)).isInstanceOf(StorageServiceUnavailableException.class);
        verifyNoInteractions(media);
        when(uploader.destroy(eq(session.publicId),anyMap())).thenReturn(Map.of("result","not found"));
        service.compensatePublication(session.id);assertThat(session.cleanupAfter).isNotNull();
        // A delayed provider upload lands after the first successful not-found response.
        when(uploader.destroy(eq(session.publicId),anyMap())).thenReturn(Map.of("result","ok"));
        service.compensatePublication(session.id);verify(uploader,times(3)).destroy(eq(session.publicId),anyMap());
        session.expiresAt=Instant.now().minusSeconds(90000);service.compensatePublication(session.id);assertThat(session.cleanupAfter).isNull();
    }
    @Test void compensationRetainsPublishedAttachedMediaAndFailsClosedOnPostOutage() {
        var session=session();session.publicationId=UUID.randomUUID();session.mediaId=UUID.randomUUID();
        when(postAccess.publication(session.publicationId)).thenReturn(new PostMediaAccess.Publication(session.publicationId,owner,"PUBLISHED",Instant.now()));
        when(postAccess.isAttached(session.mediaId)).thenReturn(true);
        service.compensatePublication(session.id);assertThat(session.cleanupAfter).isNull();verify(cloudinary,never()).uploader();
        when(postAccess.publication(session.publicationId)).thenThrow(new IllegalStateException("Post offline"));
        assertThatThrownBy(()->service.compensatePublication(session.id)).isInstanceOf(IllegalStateException.class);verify(cloudinary,never()).uploader();
    }
    @Test void pendingPublicationIsNeverCompensatedAndCannotCompleteAfterCancellation() {
        var session=session();session.publicationId=UUID.randomUUID();
        when(postAccess.publication(session.publicationId)).thenReturn(new PostMediaAccess.Publication(session.publicationId,owner,"PENDING",Instant.now().plusSeconds(3600)));
        service.compensatePublication(session.id);verify(cloudinary,never()).uploader();
        when(postAccess.publication(session.publicationId)).thenReturn(new PostMediaAccess.Publication(session.publicationId,owner,"CANCELLED",Instant.now()));
        assertThatThrownBy(()->service.complete(session.id)).isInstanceOf(ResponseStatusException.class);verifyNoInteractions(provider,media);
    }

}
