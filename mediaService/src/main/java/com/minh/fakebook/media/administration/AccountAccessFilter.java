package com.minh.fakebook.media.administration;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

/** Installed only in the security chain, after JWT authentication. No allow cache. */
public class AccountAccessFilter extends OncePerRequestFilter {
    private final AccountDirectory directory;
    public AccountAccessFilter(AccountDirectory directory) { this.directory = directory; }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (auth instanceof JwtAuthenticationToken jwt) {
            boolean internal = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_INTERNAL"));
            if (internal) {
                if (!path.startsWith("/api/internal/")) { reject(response,403,"INTERNAL_ONLY"); return; }
            } else {
                if (path.startsWith("/api/internal/")) { reject(response,403,"INTERNAL_REQUIRED"); return; }
                AccountState state;
                try { state=directory.state(jwt.getToken().getSubject()); }
                catch (Exception unavailable) { reject(response,503,"ACCOUNT_STATE_UNAVAILABLE"); return; }
                if (state==null) { reject(response,503,"ACCOUNT_STATE_UNAVAILABLE"); return; }
                boolean admin=auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
                String denial=state.denial(jwt.getToken().getIssuedAt(),admin,(path.startsWith("/api/admin/") || path.startsWith("/management/") || path.startsWith("/v3/api-docs")),path.equals("/api/account-state"));
                if (denial!=null) { reject(response,403,denial); return; }
            }
        }
        chain.doFilter(request,response);
    }
    private static void reject(HttpServletResponse response,int status,String code) throws IOException {
        response.setStatus(status); response.setContentType("application/problem+json");
        response.getWriter().write("{\"status\":"+status+",\"code\":\""+code+"\"}");
    }
}
