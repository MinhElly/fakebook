package com.minh.fakebook.media.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.minh.fakebook.media.service.FileStorageService;
import com.minh.fakebook.media.service.dto.FileUploadResult;
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
    private static final Logger LOG = LoggerFactory.getLogger(CloudinaryStorageServiceImpl.class);

    private final Cloudinary cloudinary;

    public CloudinaryStorageServiceImpl(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    @Override
    public FileUploadResult uploadFile(MultipartFile file, String folder) throws IOException {
        LOG.debug("Request to upload file to Cloudinary: {}", file.getOriginalFilename());

        try {
            //configure folder and auto detection of resource type
            Map<String, Object> params = new java.util.HashMap<>();
            params.put("folder", folder);
            params.put("resource_type", "auto");

            // Upload the file to Cloudinary
            Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), params);

            //extract results
            String secureUrl = uploadResult.get("secure_url").toString();
            String publicId = uploadResult.get("public_id").toString();

            LOG.debug("Upload sucessful! URL: {}, Key: {}", secureUrl, publicId);
            return new FileUploadResult(secureUrl, publicId);
        } catch (Exception e) {
            LOG.error("Cloudinary upload failed: {}", e.getMessage(), e);
            throw new RuntimeException("Cloudinary upload failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteFile(String storageKey) throws IOException {
        LOG.debug("Request to delete file from Cloudinary with key: {}", storageKey);
        if (storageKey == null || storageKey.isBlank()) {
            LOG.warn("Cannot delete file: storageKey is empty");
            return;
        }
        Map<?, ?> result = cloudinary.uploader().destroy(storageKey, ObjectUtils.emptyMap());
        LOG.debug("Cloudinary destroy result for key {}: {}", storageKey, result);
    }
}
