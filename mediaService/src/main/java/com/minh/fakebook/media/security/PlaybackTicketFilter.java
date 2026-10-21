package com.minh.fakebook.media.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;
import org.springframework.web.filter.OncePerRequestFilter;

/** Injects a scoped ticket's JWT before the regular JWT and account-state filters. */
public class PlaybackTicketFilter extends OncePerRequestFilter {
    private final PlaybackTickets tickets;
    public PlaybackTicketFilter(PlaybackTickets tickets) { this.tickets = tickets; }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws IOException, ServletException {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (("GET".equals(request.getMethod()) || "HEAD".equals(request.getMethod())) && request.getHeader("Authorization") == null
            && path.matches("/api/media/[0-9a-fA-F-]{36}/file")) {
            String bearer;
            try { bearer = tickets.bearer(UUID.fromString(path.split("/")[3]), request.getParameter("ticket")); }
            catch (IllegalArgumentException invalid) { bearer = null; }
            if (bearer != null) {
                final String header = "Bearer " + bearer;
                request = new HttpServletRequestWrapper(request) {
                    @Override public String getHeader(String name) { return "Authorization".equalsIgnoreCase(name) ? header : super.getHeader(name); }
                    @Override public Enumeration<String> getHeaders(String name) { return "Authorization".equalsIgnoreCase(name) ? Collections.enumeration(List.of(header)) : super.getHeaders(name); }
                };
            }
        }
        chain.doFilter(request, response);
    }
}
