package com.minh.fakebook.feed.administration;

import java.time.Duration;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.security.oauth2.client.*;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.http.client.JdkClientHttpRequestFactory;
@Component
public class InternalHttp {
    private final OAuth2AuthorizedClientManager manager;
    private final RestClient client;
    public InternalHttp(ClientRegistrationRepository registrations, OAuth2AuthorizedClientService clients) {
        var serviceManager=new AuthorizedClientServiceOAuth2AuthorizedClientManager(registrations,clients);
        serviceManager.setAuthorizedClientProvider(OAuth2AuthorizedClientProviderBuilder.builder().clientCredentials().build());
        this.manager=serviceManager;
        var factory=new JdkClientHttpRequestFactory(java.net.http.HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
        factory.setReadTimeout(Duration.ofSeconds(5));
        this.client=RestClient.builder().requestFactory(factory).build();
    }
    public RestClient client() { return client; }
    public String token() {
        var authorized=manager.authorize(OAuth2AuthorizeRequest.withClientRegistrationId("internal").principal("account-control").build());
        if (authorized==null) throw new IllegalStateException("Internal token unavailable");
        return authorized.getAccessToken().getTokenValue();
    }
}
