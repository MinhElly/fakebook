package com.minh.fakebook.media.service;

import com.minh.fakebook.media.repository.MediaUploadSessionRepository;
import java.time.Instant;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DirectUploadCleanup {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(DirectUploadCleanup.class);
    private final MediaUploadSessionRepository sessions;
    private final DirectUploadService uploads;
    private final MediaService media;
    public DirectUploadCleanup(MediaUploadSessionRepository sessions, DirectUploadService uploads, MediaService media) {
        this.sessions = sessions; this.uploads = uploads;
        this.media = media;
    }
    @Scheduled(fixedDelayString = "${media.direct-upload.cleanup-delay-ms:3600000}", initialDelay = 60000)
    public void clean() {
        for (var session : sessions.findTop50ByMediaIdIsNullAndExpiresAtBeforeOrderByExpiresAtAsc(Instant.now().minusSeconds(86400))) {
            if (session.publicationId != null) continue;
            try { uploads.cleanExpiredSession(session.id); }
            catch (RuntimeException error) { LOG.warn("Direct upload cleanup will retry session {}", session.id); }
        }
        for (var session : sessions.findTop50ByMediaIdIsNotNullAndExpiresAtBeforeOrderByExpiresAtAsc(Instant.now().minusSeconds(86400))) {
            if (session.publicationId != null) continue;
            try {
                // MediaService rechecks current Post references; a linked asset is never deleted by this sweep.
                media.deleteBySystem(session.mediaId);
                sessions.deleteById(session.id);
            } catch (RuntimeException error) { LOG.warn("Completed upload cleanup will retry session {}", session.id); }
        }
    }
    @Scheduled(fixedDelayString="${media.publication.cleanup-delay-ms:10000}", initialDelay=15000)
    public void compensatePublications() {
        for (var session : sessions.findTop50ByPublicationIdIsNotNullAndCleanupAfterBeforeOrderByCleanupAfterAsc(Instant.now())) {
            try { uploads.compensatePublication(session.id); }
            catch (RuntimeException error) {
                try { uploads.deferCompensation(session.id); } catch (RuntimeException ignored) { /* next sweep retries */ }
                LOG.warn("Publication compensation will retry upload {}", session.id);
            }
        }
    }

}
