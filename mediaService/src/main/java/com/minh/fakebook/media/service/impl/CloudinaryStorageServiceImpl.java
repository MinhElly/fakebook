package com.minh.fakebook.media.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.minh.fakebook.media.service.FileStorageService;
import com.minh.fakebook.media.service.StorageServiceUnavailableException;
import com.minh.fakebook.media.service.dto.FileUploadResult;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Service implementation for uploading files to Cloudinary.
 */

@Service
public class CloudinaryStorageServiceImpl implements FileStorageService {
    private static final String CLOUDINARY_CIRCUIT = "cloudinary";
    private static final Logger LOG = LoggerFactory.getLogger(CloudinaryStorageServiceImpl.class);

    private final Cloudinary cloudinary;

    public CloudinaryStorageServiceImpl(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    @Override
    @CircuitBreaker(name = CLOUDINARY_CIRCUIT, fallbackMethod = "uploadFileFallback")
    public FileUploadResult uploadFile(MultipartFile file, String folder) throws IOException {
        LOG.debug("Request to upload file to Cloudinary: {}", file.getOriginalFilename());

        Map<String, Object> params = new HashMap<>();
        params.put("folder", folder);
        params.put("resource_type", "auto");

        Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), params);
        Object secureUrl = uploadResult.get("secure_url");
        Object publicId = uploadResult.get("public_id");
        if (!(secureUrl instanceof String) || !(publicId instanceof String)) {
            throw new IOException("Cloudinary upload response did not contain the expected fields");
        }

        LOG.debug("Upload successful! URL: {}, Key: {}", secureUrl, publicId);
        return new FileUploadResult((String) secureUrl, (String) publicId);
    }

    @Override
    @CircuitBreaker(name = CLOUDINARY_CIRCUIT, fallbackMethod = "deleteFileFallback")
    public void deleteFile(String storageKey) throws IOException {
        LOG.debug("Request to delete file from Cloudinary with key: {}", storageKey);
        if (storageKey == null || storageKey.isBlank()) {
            LOG.warn("Cannot delete file: storageKey is empty");
            return;
        }
        Map<?, ?> result = cloudinary.uploader().destroy(storageKey, ObjectUtils.emptyMap());
        if (!"ok".equals(result.get("result")) && !"not found".equals(result.get("result"))) {
            throw new IOException("Cloudinary did not confirm deletion for " + storageKey);
        }
        LOG.debug("Cloudinary destroy result for key {}: {}", storageKey, result);
    }

    FileUploadResult uploadFileFallback(MultipartFile file, String folder, Throwable cause) {
        logFallback("upload", cause);
        throw new StorageServiceUnavailableException("upload", cause);
    }

    void deleteFileFallback(String storageKey, Throwable cause) {
        logFallback("delete", cause);
        throw new StorageServiceUnavailableException("delete", cause);
    }

    private void logFallback(String operation, Throwable cause) {
        if (cause instanceof CallNotPermittedException) {
            LOG.warn("Cloudinary circuit is open; rejecting {} request", operation);
        } else {
            LOG.error("Cloudinary {} failed; circuit breaker fallback invoked", operation, cause);
        }
    }
}
