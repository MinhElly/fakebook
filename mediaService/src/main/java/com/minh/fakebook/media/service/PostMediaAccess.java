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
    private final com.minh.fakebook.media.administration.InternalHttp internal;
    public PostMediaAccess(DiscoveryClient discovery,
            @org.springframework.beans.factory.annotation.Value("${POST_SERVICE_URL:http://postservice:8083}") String postUrl,
            com.minh.fakebook.media.administration.InternalHttp internal) {
        this.discovery = discovery;
        this.postUrl = postUrl;
        this.internal = internal;
        var factory = new SimpleClientHttpRequestFactory(); factory.setConnectTimeout(2000); factory.setReadTimeout(3000);
        client = RestClient.builder().requestFactory(factory).build();
    }
    public record Grant(boolean attached,boolean readable) {}
    public java.util.Map<UUID, Grant> batch(java.util.List<UUID> ids) {
        var request = client.post().uri(postUrl + "/api/posts/media/access-batch");
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwt) request.header("Authorization", "Bearer " + jwt.getToken().getTokenValue());
        try {
            var result = request.body(ids).retrieve().body(new org.springframework.core.ParameterizedTypeReference<java.util.Map<UUID, Grant>>() {});
            if (result == null) throw new org.springframework.web.server.ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Post authorization unavailable");
            return result;
        } catch (RestClientException error) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Post authorization unavailable", error);
        }
    }
    public record Publication(UUID id, UUID ownerId, String status, java.time.Instant expiresAt) {}
    public Publication publication(UUID id) {
        var value = internal.client().get().uri(postUrl + "/api/internal/publications/" + id)
            .headers(headers -> headers.setBearerAuth(internal.token())).retrieve().body(Publication.class);
        if (value == null) throw new IllegalStateException("Publication status unavailable");
        return value;
    }
    public boolean isAttached(UUID mediaId) {
        Boolean attached = internal.client().get().uri(postUrl + "/api/internal/media/" + mediaId + "/attached")
            .headers(headers -> headers.setBearerAuth(internal.token())).retrieve().body(Boolean.class);
        if (attached == null) throw new IllegalStateException("Media reference check unavailable");
        return attached;
    }
    public boolean canOwnerRead(UUID mediaId) {
        var instances=discovery.getInstances("postservice");
        var request=client.get().uri((instances.isEmpty()?postUrl:instances.getFirst().getUri().toString())+"/api/posts/media/"+mediaId+"/attachment-access");
        var auth=SecurityContextHolder.getContext().getAuthentication();
        if(auth instanceof JwtAuthenticationToken jwt) request.header("Authorization","Bearer "+jwt.getToken().getTokenValue());
        try {
            Grant grant=request.retrieve().body(Grant.class);
            return grant!=null && (!grant.attached() || grant.readable());
        } catch(RestClientException error) {throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Post authorization unavailable",error);}
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
