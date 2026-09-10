package com.minh.fakebook.media.service.dto;

/**
 * Record holding the result of a file upload operation.
 *
 * @param url        The publicly accessible URL of the file.
 * @param storageKey The internal key used by the provider (e.g., public_id in Cloudinary).
 */
public record FileUploadResult(String url, String storageKey) {
}

