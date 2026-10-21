package com.minh.fakebook.media.repository;

import com.minh.fakebook.media.domain.MediaUploadSession;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;

public interface MediaUploadSessionRepository extends JpaRepository<MediaUploadSession, UUID> {
    java.util.List<MediaUploadSession> findTop50ByPublicationIdIsNotNullAndCleanupAfterBeforeOrderByCleanupAfterAsc(java.time.Instant now);
    java.util.List<MediaUploadSession> findTop50ByMediaIdIsNullAndExpiresAtBeforeOrderByExpiresAtAsc(java.time.Instant cutoff);
    java.util.List<MediaUploadSession> findTop50ByMediaIdIsNotNullAndExpiresAtBeforeOrderByExpiresAtAsc(java.time.Instant cutoff);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from MediaUploadSession s where s.id = :id")
    Optional<MediaUploadSession> findForCompletion(@org.springframework.data.repository.query.Param("id") UUID id);
}
