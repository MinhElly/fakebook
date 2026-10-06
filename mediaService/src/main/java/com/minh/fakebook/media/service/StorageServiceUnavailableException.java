package com.minh.fakebook.media.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class StorageServiceUnavailableException extends RuntimeException {

    public StorageServiceUnavailableException(String operation, Throwable cause) {
        super("Cloudinary unavailable during " + operation, cause);
    }
}
