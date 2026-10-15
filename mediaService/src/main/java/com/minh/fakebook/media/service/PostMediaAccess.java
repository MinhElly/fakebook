package com.minh.fakebook.media.service;
import java.util.UUID;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.client.*;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PostMediaAccess {
    private final DiscoveryClient discovery;
    private final RestClient client;
    private final String postUrl;
    public PostMediaAccess(DiscoveryClient discovery,
            @org.springframework.beans.factory.annotation.Value("${POST_SERVICE_URL:http://postservice:8083}") String postUrl) {
        this.discovery = discovery;
        this.postUrl = postUrl;
        var factory = new SimpleClientHttpRequestFactory(); factory.setConnectTimeout(2000); factory.setReadTimeout(3000);
        client = RestClient.builder().requestFactory(factory).build();
    }
    public boolean canRead(UUID mediaId) {
        var instances = discovery.getInstances("postservice");
        var request = client.get().uri((instances.isEmpty() ? postUrl : instances.getFirst().getUri().toString()) + "/api/posts/media/" + mediaId + "/access");
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwt) request.header("Authorization", "Bearer " + jwt.getToken().getTokenValue());
        try { return Boolean.TRUE.equals(request.retrieve().body(Boolean.class)); }
        catch (RestClientException error) { throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Post authorization unavailable", error); }
    }
}
