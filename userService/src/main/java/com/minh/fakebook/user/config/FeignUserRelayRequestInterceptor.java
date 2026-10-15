package com.minh.fakebook.user.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import feign.RequestInterceptor;
import feign.RequestTemplate;

@Component 
public class FeignUserRelayRequestInterceptor implements RequestInterceptor{
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_TOKEN_TYPE = "Bearer";

    @Override
    public void apply(RequestTemplate template){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication instanceof JwtAuthenticationToken jwtAuthenticationToken){
            String tokenValue = jwtAuthenticationToken.getToken().getTokenValue();
            template.header(AUTHORIZATION_HEADER, "%s %s".formatted(BEARER_TOKEN_TYPE, tokenValue));
            return ;
        }
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            String authorization = attributes.getRequest().getHeader(AUTHORIZATION_HEADER);
            if (authorization != null && authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
                template.header(AUTHORIZATION_HEADER, authorization);
            }
        }
    }
}
