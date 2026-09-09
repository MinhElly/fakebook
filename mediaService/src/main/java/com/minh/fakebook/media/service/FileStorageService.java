package com.minh.fakebook.media.service;

import com.minh.fakebook.media.service.dto.FileUploadResult;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

/**
 * Interface for handling physical file storage operations.
 */
public interface FileStorageService {
    /**
     * Uploads a file to the storage provider.
     *
     * @param file   the multipart file to upload.
     * @param folder the destination folder in the storage.
     * @return the result containing URL and storage key.
     * @throws IOException if an error occurs during byte reading.
     */

    FileUploadResult uploadFile(MultipartFile file, String folder) throws IOException;
}
