package com.minh.fakebook.user.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import feign.RequestInterceptor;
import feign.RequestTemplate;

@Component 
public class FeignUserRelayRequestInterceptor implements RequestInterceptor{
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_TOKEN_TYPE = "Bearer";

    public void apply(RequestTemplate template){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication instanceof JwtAuthenticationToken jwtAuthenticationToken){
            String tokenValue = jwtAuthenticationToken.getToken().getTokenValue();
            template.header(AUTHORIZATION_HEADER, "%s %s".formatted(BEARER_TOKEN_TYPE, tokenValue));
        }
    }
}
