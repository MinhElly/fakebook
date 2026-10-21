package com.minh.fakebook.post.service;
@org.springframework.stereotype.Component
public class PostPublicationExpiry {
    private final PostPublicationService service;
    public PostPublicationExpiry(PostPublicationService service) { this.service=service; }
    @org.springframework.scheduling.annotation.Scheduled(fixedDelayString="${post.publication.expiry-delay-ms:60000}", initialDelay=60000)
    public void expire() { service.expire(); }
}
