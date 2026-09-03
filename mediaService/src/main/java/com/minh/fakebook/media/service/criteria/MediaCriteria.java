package com.minh.fakebook.media.service.criteria;

import com.minh.fakebook.media.domain.enumeration.MediaStatus;
import com.minh.fakebook.media.domain.enumeration.MediaType;
import com.minh.fakebook.media.domain.enumeration.StorageProvider;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link com.minh.fakebook.media.domain.Media} entity. This class is used
 * in {@link com.minh.fakebook.media.web.rest.MediaResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /media?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class MediaCriteria implements Serializable, Criteria {

    /**
     * Class for filtering MediaType
     */
    public static class MediaTypeFilter extends Filter<MediaType> {

        public MediaTypeFilter() {}

        public MediaTypeFilter(MediaTypeFilter filter) {
            super(filter);
        }

        @Override
        public MediaTypeFilter copy() {
            return new MediaTypeFilter(this);
        }
    }

    /**
     * Class for filtering StorageProvider
     */
    public static class StorageProviderFilter extends Filter<StorageProvider> {

        public StorageProviderFilter() {}

        public StorageProviderFilter(StorageProviderFilter filter) {
            super(filter);
        }

        @Override
        public StorageProviderFilter copy() {
            return new StorageProviderFilter(this);
        }
    }

    /**
     * Class for filtering MediaStatus
     */
    public static class MediaStatusFilter extends Filter<MediaStatus> {

        public MediaStatusFilter() {}

        public MediaStatusFilter(MediaStatusFilter filter) {
            super(filter);
        }

        @Override
        public MediaStatusFilter copy() {
            return new MediaStatusFilter(this);
        }
    }

    @Serial
    private static final long serialVersionUID = 1L;

    private UUIDFilter id;

    private UUIDFilter ownerId;

    private StringFilter fileName;

    private MediaTypeFilter mediaType;

    private StringFilter mimeType;

    private LongFilter fileSize;

    private StorageProviderFilter storageProvider;

    private StringFilter storageKey;

    private StringFilter url;

    private MediaStatusFilter status;

    private InstantFilter createdAt;

    private InstantFilter updatedAt;

    private Boolean distinct;

    public MediaCriteria() {}

    public MediaCriteria(MediaCriteria other) {
        this.id = other.optionalId().map(UUIDFilter::copy).orElse(null);
        this.ownerId = other.optionalOwnerId().map(UUIDFilter::copy).orElse(null);
        this.fileName = other.optionalFileName().map(StringFilter::copy).orElse(null);
        this.mediaType = other.optionalMediaType().map(MediaTypeFilter::copy).orElse(null);
        this.mimeType = other.optionalMimeType().map(StringFilter::copy).orElse(null);
        this.fileSize = other.optionalFileSize().map(LongFilter::copy).orElse(null);
        this.storageProvider = other.optionalStorageProvider().map(StorageProviderFilter::copy).orElse(null);
        this.storageKey = other.optionalStorageKey().map(StringFilter::copy).orElse(null);
        this.url = other.optionalUrl().map(StringFilter::copy).orElse(null);
        this.status = other.optionalStatus().map(MediaStatusFilter::copy).orElse(null);
        this.createdAt = other.optionalCreatedAt().map(InstantFilter::copy).orElse(null);
        this.updatedAt = other.optionalUpdatedAt().map(InstantFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public MediaCriteria copy() {
        return new MediaCriteria(this);
    }

    public UUIDFilter getId() {
        return id;
    }

    public Optional<UUIDFilter> optionalId() {
        return Optional.ofNullable(id);
    }

    public UUIDFilter id() {
        if (id == null) {
            setId(new UUIDFilter());
        }
        return id;
    }

    public void setId(UUIDFilter id) {
        this.id = id;
    }

    public UUIDFilter getOwnerId() {
        return ownerId;
    }

    public Optional<UUIDFilter> optionalOwnerId() {
        return Optional.ofNullable(ownerId);
    }

    public UUIDFilter ownerId() {
        if (ownerId == null) {
            setOwnerId(new UUIDFilter());
        }
        return ownerId;
    }

    public void setOwnerId(UUIDFilter ownerId) {
        this.ownerId = ownerId;
    }

    public StringFilter getFileName() {
        return fileName;
    }

    public Optional<StringFilter> optionalFileName() {
        return Optional.ofNullable(fileName);
    }

    public StringFilter fileName() {
        if (fileName == null) {
            setFileName(new StringFilter());
        }
        return fileName;
    }

    public void setFileName(StringFilter fileName) {
        this.fileName = fileName;
    }

    public MediaTypeFilter getMediaType() {
        return mediaType;
    }

    public Optional<MediaTypeFilter> optionalMediaType() {
        return Optional.ofNullable(mediaType);
    }

    public MediaTypeFilter mediaType() {
        if (mediaType == null) {
            setMediaType(new MediaTypeFilter());
        }
        return mediaType;
    }

    public void setMediaType(MediaTypeFilter mediaType) {
        this.mediaType = mediaType;
    }

    public StringFilter getMimeType() {
        return mimeType;
    }

    public Optional<StringFilter> optionalMimeType() {
        return Optional.ofNullable(mimeType);
    }

    public StringFilter mimeType() {
        if (mimeType == null) {
            setMimeType(new StringFilter());
        }
        return mimeType;
    }

    public void setMimeType(StringFilter mimeType) {
        this.mimeType = mimeType;
    }

    public LongFilter getFileSize() {
        return fileSize;
    }

    public Optional<LongFilter> optionalFileSize() {
        return Optional.ofNullable(fileSize);
    }

    public LongFilter fileSize() {
        if (fileSize == null) {
            setFileSize(new LongFilter());
        }
        return fileSize;
    }

    public void setFileSize(LongFilter fileSize) {
        this.fileSize = fileSize;
    }

    public StorageProviderFilter getStorageProvider() {
        return storageProvider;
    }

    public Optional<StorageProviderFilter> optionalStorageProvider() {
        return Optional.ofNullable(storageProvider);
    }

    public StorageProviderFilter storageProvider() {
        if (storageProvider == null) {
            setStorageProvider(new StorageProviderFilter());
        }
        return storageProvider;
    }

    public void setStorageProvider(StorageProviderFilter storageProvider) {
        this.storageProvider = storageProvider;
    }

    public StringFilter getStorageKey() {
        return storageKey;
    }

    public Optional<StringFilter> optionalStorageKey() {
        return Optional.ofNullable(storageKey);
    }

    public StringFilter storageKey() {
        if (storageKey == null) {
            setStorageKey(new StringFilter());
        }
        return storageKey;
    }

    public void setStorageKey(StringFilter storageKey) {
        this.storageKey = storageKey;
    }

    public StringFilter getUrl() {
        return url;
    }

    public Optional<StringFilter> optionalUrl() {
        return Optional.ofNullable(url);
    }

    public StringFilter url() {
        if (url == null) {
            setUrl(new StringFilter());
        }
        return url;
    }

    public void setUrl(StringFilter url) {
        this.url = url;
    }

    public MediaStatusFilter getStatus() {
        return status;
    }

    public Optional<MediaStatusFilter> optionalStatus() {
        return Optional.ofNullable(status);
    }

    public MediaStatusFilter status() {
        if (status == null) {
            setStatus(new MediaStatusFilter());
        }
        return status;
    }

    public void setStatus(MediaStatusFilter status) {
        this.status = status;
    }

    public InstantFilter getCreatedAt() {
        return createdAt;
    }

    public Optional<InstantFilter> optionalCreatedAt() {
        return Optional.ofNullable(createdAt);
    }

    public InstantFilter createdAt() {
        if (createdAt == null) {
            setCreatedAt(new InstantFilter());
        }
        return createdAt;
    }

    public void setCreatedAt(InstantFilter createdAt) {
        this.createdAt = createdAt;
    }

    public InstantFilter getUpdatedAt() {
        return updatedAt;
    }

    public Optional<InstantFilter> optionalUpdatedAt() {
        return Optional.ofNullable(updatedAt);
    }

    public InstantFilter updatedAt() {
        if (updatedAt == null) {
            setUpdatedAt(new InstantFilter());
        }
        return updatedAt;
    }

    public void setUpdatedAt(InstantFilter updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Boolean getDistinct() {
        return distinct;
    }

    public Optional<Boolean> optionalDistinct() {
        return Optional.ofNullable(distinct);
    }

    public Boolean distinct() {
        if (distinct == null) {
            setDistinct(true);
        }
        return distinct;
    }

    public void setDistinct(Boolean distinct) {
        this.distinct = distinct;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final MediaCriteria that = (MediaCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(ownerId, that.ownerId) &&
            Objects.equals(fileName, that.fileName) &&
            Objects.equals(mediaType, that.mediaType) &&
            Objects.equals(mimeType, that.mimeType) &&
            Objects.equals(fileSize, that.fileSize) &&
            Objects.equals(storageProvider, that.storageProvider) &&
            Objects.equals(storageKey, that.storageKey) &&
            Objects.equals(url, that.url) &&
            Objects.equals(status, that.status) &&
            Objects.equals(createdAt, that.createdAt) &&
            Objects.equals(updatedAt, that.updatedAt) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            id,
            ownerId,
            fileName,
            mediaType,
            mimeType,
            fileSize,
            storageProvider,
            storageKey,
            url,
            status,
            createdAt,
            updatedAt,
            distinct
        );
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "MediaCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalOwnerId().map(f -> "ownerId=" + f + ", ").orElse("") +
            optionalFileName().map(f -> "fileName=" + f + ", ").orElse("") +
            optionalMediaType().map(f -> "mediaType=" + f + ", ").orElse("") +
            optionalMimeType().map(f -> "mimeType=" + f + ", ").orElse("") +
            optionalFileSize().map(f -> "fileSize=" + f + ", ").orElse("") +
            optionalStorageProvider().map(f -> "storageProvider=" + f + ", ").orElse("") +
            optionalStorageKey().map(f -> "storageKey=" + f + ", ").orElse("") +
            optionalUrl().map(f -> "url=" + f + ", ").orElse("") +
            optionalStatus().map(f -> "status=" + f + ", ").orElse("") +
            optionalCreatedAt().map(f -> "createdAt=" + f + ", ").orElse("") +
            optionalUpdatedAt().map(f -> "updatedAt=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
