package com.minh.fakebook.feed.client;

public class PostServiceUnavailableException extends RuntimeException {

    public PostServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
