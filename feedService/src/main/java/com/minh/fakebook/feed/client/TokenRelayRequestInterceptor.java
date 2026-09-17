package com.minh.fakebook.feed.client;

import feign.RequestInterceptor;
import feign.RequestTemplate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class TokenRelayRequestInterceptor implements RequestInterceptor {

    private static final Logger LOG = LoggerFactory.getLogger(TokenRelayRequestInterceptor.class);

    private final OAuth2AuthorizedClientManager authorizedClientManager;

    public TokenRelayRequestInterceptor(OAuth2AuthorizedClientManager authorizedClientManager) {
        this.authorizedClientManager = authorizedClientManager;
    }

    @Override
    public void apply(RequestTemplate template) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            String tokenValue = jwtAuth.getToken().getTokenValue();
            template.header("Authorization", "Bearer " + tokenValue);
            return;
        }
        try{
            OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest.withClientRegistrationId("oidc")
                .principal("feed-service-internal")
                .build();
            OAuth2AuthorizedClient authorizedClient = authorizedClientManager.authorize(authorizeRequest);
            if(authorizedClient != null && authorizedClient.getAccessToken() != null){
                String serviceToken = authorizedClient.getAccessToken().getTokenValue();
                template.header("Authorization", "Bearer " + serviceToken);
                LOG.debug("Attached Client Credentials M2M token for internal Feign request.");
                return;
            }else {
                LOG.warn("Could not acquire M2M token for internal Feign request.");
            }
            
        }catch(Exception ex){
            LOG.error("Error while acquiring M2M token for internal Feign request.", ex.getMessage());
        }
    }
}
