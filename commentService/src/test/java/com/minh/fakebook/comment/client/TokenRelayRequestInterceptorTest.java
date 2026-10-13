package com.minh.fakebook.comment.client;

import static org.assertj.core.api.Assertions.assertThat;

import feign.RequestTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class TokenRelayRequestInterceptorTest {

    @AfterEach
    void clearContexts() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void relaysBearerOnCircuitBreakerWorkerWithoutSecurityContext() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer caller-token");
        ServletRequestAttributes attributes = new ServletRequestAttributes(request);
        try (var executor = java.util.concurrent.Executors.newSingleThreadExecutor()) {
            RequestTemplate template = executor.submit(() -> {
                assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
                RequestContextHolder.setRequestAttributes(attributes);
                try {
                    RequestTemplate result = new RequestTemplate();
                    new TokenRelayRequestInterceptor().apply(result);
                    return result;
                } finally {
                    RequestContextHolder.resetRequestAttributes();
                }
            }).get();
            assertThat(template.headers().get("Authorization")).containsExactly("Bearer caller-token");
            assertThat(executor.submit(RequestContextHolder::getRequestAttributes).get()).isNull();
        }
    }

    @Test
    void doesNotRelayNonBearerCredentials() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic other-credentials");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        RequestTemplate template = new RequestTemplate();
        new TokenRelayRequestInterceptor().apply(template);
        assertThat(template.headers()).doesNotContainKey("Authorization");
    }

    @Test
    void doesNotInventCredentialsWithoutRequestContext() {
        RequestTemplate template = new RequestTemplate();
        new TokenRelayRequestInterceptor().apply(template);
        assertThat(template.headers()).doesNotContainKey("Authorization");
    }
}
