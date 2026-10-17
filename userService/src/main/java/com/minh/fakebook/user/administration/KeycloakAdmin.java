package com.minh.fakebook.user.administration;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

/** Confidential realm-management client. Its credentials are never returned by any resource. */
@Component
public class KeycloakAdmin {
    private final RestClient http;
    private final String issuer;
    private final String adminBase;
    private final String clientId;
    private final String secret;
    public KeycloakAdmin(@Value("${spring.security.oauth2.client.provider.oidc.issuer-uri}") String issuer,
        @Value("${FAKEBOOK_ADMIN_CLIENT_ID:fakebook-administration}") String clientId,
        @Value("${FAKEBOOK_ADMIN_CLIENT_SECRET:}") String secret) {
        this.issuer=issuer.replaceAll("/$", ""); this.clientId=clientId; this.secret=secret;
        int realm=this.issuer.lastIndexOf("/realms/");
        if (realm<0) throw new IllegalArgumentException("Keycloak realm issuer required");
        this.adminBase=this.issuer.substring(0,realm)+"/admin"+this.issuer.substring(realm);
        var factory=new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
        factory.setReadTimeout(Duration.ofSeconds(10));this.http=RestClient.builder().requestFactory(factory).build();
    }
    private String token() {
        if (secret.isBlank()) throw new IllegalStateException("FAKEBOOK_ADMIN_CLIENT_SECRET is required");
        var form=new LinkedMultiValueMap<String,String>();
        form.add("grant_type","client_credentials");form.add("client_id",clientId);form.add("client_secret",secret);
        var body=http.post().uri(issuer+"/protocol/openid-connect/token").contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(form).retrieve().body(new ParameterizedTypeReference<Map<String,Object>>() {});
        if (body==null || body.get("access_token")==null) throw new IllegalStateException("Keycloak admin token unavailable");
        return body.get("access_token").toString();
    }
    public Map<String,Object> identity(String subject) {
        var user=http.get().uri(adminBase+"/users/{id}",UUID.fromString(subject)).headers(h -> h.setBearerAuth(token()))
            .retrieve().body(new ParameterizedTypeReference<Map<String,Object>>() {});
        if (user==null) throw new IllegalStateException("Keycloak identity unavailable");
        return user;
    }
    public boolean isAdmin(String subject) {
        var roles=http.get().uri(adminBase+"/users/{id}/role-mappings/realm/composite",UUID.fromString(subject))
            .headers(h -> h.setBearerAuth(token())).retrieve().body(new ParameterizedTypeReference<List<Map<String,Object>>>() {});
        if (roles==null) throw new IllegalStateException("Keycloak roles unavailable");
        return roles.stream().anyMatch(role -> "ROLE_ADMIN".equals(role.get("name")));
    }
    public List<Map<String,Object>> users(int first,int max) {
        var users=http.get().uri(adminBase+"/users?first={first}&max={max}",first,max).headers(h -> h.setBearerAuth(token()))
            .retrieve().body(new ParameterizedTypeReference<List<Map<String,Object>>>() {});
        if (users==null) throw new IllegalStateException("Keycloak directory unavailable");
        return users;
    }
    public void synchronize(String subject,String action) {
        String uri=adminBase+"/users/"+UUID.fromString(subject);
        if (action.equals("GRANT_ADMIN") || action.equals("REVOKE_ADMIN")) {
            var role=http.get().uri(adminBase+"/roles/ROLE_ADMIN").headers(h -> h.setBearerAuth(token()))
                .retrieve().body(new ParameterizedTypeReference<Map<String,Object>>() {});
            http.method(action.equals("GRANT_ADMIN")?org.springframework.http.HttpMethod.POST:org.springframework.http.HttpMethod.DELETE)
                .uri(uri+"/role-mappings/realm").headers(h -> h.setBearerAuth(token())).body(List.of(Objects.requireNonNull(role))).retrieve().toBodilessEntity();
            if (isAdmin(subject)!=action.equals("GRANT_ADMIN")) throw new IllegalStateException("Inherited ROLE_ADMIN must be removed before revocation can complete");
        } else {
            http.put().uri(uri).headers(h -> h.setBearerAuth(token())).body(Map.of("enabled",action.equals("UNLOCK"))).retrieve().toBodilessEntity();
        }
        http.post().uri(uri+"/logout").headers(h -> h.setBearerAuth(token())).retrieve().toBodilessEntity();
    }
}
