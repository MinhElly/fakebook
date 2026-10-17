package com.minh.fakebook.gateway.administration;

import org.springframework.web.server.*;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import reactor.core.publisher.Mono;
public class AccountAccessWebFilter implements WebFilter {
    private final AccountDirectory directory;
    public AccountAccessWebFilter(AccountDirectory directory) {this.directory=directory;}
    @Override public Mono<Void> filter(ServerWebExchange exchange,WebFilterChain chain) {
        String path=exchange.getRequest().getPath().value();
        if (!path.startsWith("/api/") && !path.startsWith("/services/") && !path.startsWith("/management/")) return chain.filter(exchange);
        return exchange.getPrincipal().filter(Authentication.class::isInstance).cast(Authentication.class)
            .flatMap(auth -> authorize(exchange,auth,path).thenReturn(true)
                .onErrorResume(ResponseStatusException.class,error -> {
                    exchange.getResponse().setStatusCode(error.getStatusCode());
                    exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);
                    String body="{\"status\":"+error.getStatusCode().value()+",\"code\":\""+error.getReason()+"\"}";
                    return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(body.getBytes(java.nio.charset.StandardCharsets.UTF_8)))).thenReturn(false);
                })).defaultIfEmpty(true)
            .flatMap(allowed -> allowed?chain.filter(exchange):Mono.empty());
    }
    private Mono<Void> authorize(ServerWebExchange exchange,Authentication auth,String path) {
        boolean internal=auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_INTERNAL"));
        boolean internalPath=path.matches("/services/[^/]+/api/internal/.*") || path.startsWith("/api/internal/");
        if(internal) return internalPath?Mono.empty():deny("INTERNAL_ONLY");
        if(internalPath) return deny("INTERNAL_REQUIRED");
        String subject;java.time.Instant issued;
        if(auth instanceof JwtAuthenticationToken jwt) { subject=jwt.getToken().getSubject();issued=jwt.getToken().getIssuedAt(); }
        else if(auth.getPrincipal() instanceof OidcUser oidc) {subject=oidc.getSubject();issued=oidc.getIssuedAt();}
        else return deny("JWT_REQUIRED");
        boolean admin=auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean adminPath=path.startsWith("/api/admin/") || path.matches("/services/[^/]+/api/admin/.*") || path.startsWith("/management/") || path.contains("/v3/api-docs") || path.matches("/services/[^/]+/management/.*");
        boolean accountPath=path.endsWith("/api/account-state") || path.equals("/api/account") || path.equals("/api/logout");
        return directory.state(subject).flatMap(state -> {
            String reason=state.denial(issued,admin,adminPath,accountPath);
            return reason==null?Mono.empty():deny(reason);
        });
    }
    private Mono<Void> deny(String code) {return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN,code));}
}
