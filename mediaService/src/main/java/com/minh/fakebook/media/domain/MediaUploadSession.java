package com.minh.fakebook.media.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Durable reservation: clients can complete only an asset reserved for themselves. */
@Entity
@Table(name = "media_upload_session")
public class MediaUploadSession {
    @Id @JdbcTypeCode(SqlTypes.VARCHAR) @Column(length = 36)
    public UUID id;
    @JdbcTypeCode(SqlTypes.VARCHAR) @Column(name = "owner_id", length = 36, nullable = false)
    public UUID ownerId;
    @Column(name = "public_id", nullable = false, unique = true)
    public String publicId;
    @Column(name = "file_name", nullable = false)
    public String fileName;
    @Column(name = "resource_type", nullable = false)
    public String resourceType = "image";
    @Column(name = "expires_at", nullable = false)
    public Instant expiresAt;
    @JdbcTypeCode(SqlTypes.VARCHAR) @Column(name = "media_id", length = 36)
    public UUID mediaId;
    @JdbcTypeCode(SqlTypes.VARCHAR) @Column(name="publication_id", length=36)
    public UUID publicationId;
    @Column(name="cleanup_after")
    public Instant cleanupAfter;
}
