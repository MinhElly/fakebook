package com.minh.fakebook.user.config;

import static org.mockito.Mockito.mock;

import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.jwt.JwtDecoder;

/**
 * This class allows you to run unit and integration tests without an IdP.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestSecurityConfiguration {

    /** Legacy service tests stub the new remote directory boundary, never bypass its security filter. */
    @Bean
    @org.springframework.context.annotation.Primary
    com.minh.fakebook.user.administration.AccountDirectory accountDirectoryFixture() {
        var directory=org.mockito.Mockito.mock(com.minh.fakebook.user.administration.AccountDirectory.class);
        org.mockito.Mockito.when(directory.excluded()).thenReturn(java.util.List.of());
        org.mockito.Mockito.when(directory.state(org.mockito.ArgumentMatchers.anyString())).thenAnswer(call -> {
            var auth=org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            boolean admin=auth!=null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            return new com.minh.fakebook.user.administration.AccountState(call.getArgument(0),admin?"ADMIN":"USER","ACTIVE",null,false,0,"SYNCED");
        });
        return directory;
    }

    @Bean
    ClientRegistration clientRegistration() {
        return clientRegistrationBuilder().build();
    }

    @Bean
    ClientRegistrationRepository clientRegistrationRepository(ClientRegistration clientRegistration) {
        return new InMemoryClientRegistrationRepository(clientRegistration);
    }

    private ClientRegistration.Builder clientRegistrationBuilder() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("end_session_endpoint", "https://jhipster.org/logout");

        return ClientRegistration.withRegistrationId("oidc")
            .issuerUri("{baseUrl}")
            .redirectUri("{baseUrl}/{action}/oauth2/code/{registrationId}")
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .scope("read:user")
            .authorizationUri("https://jhipster.org/login/oauth/authorize")
            .tokenUri("https://jhipster.org/login/oauth/access_token")
            .jwkSetUri("https://jhipster.org/oauth/jwk")
            .userInfoUri("https://api.jhipster.org/user")
            .providerConfigurationMetadata(metadata)
            .userNameAttributeName("id")
            .clientName("Client Name")
            .clientId("client-id")
            .clientSecret("client-secret");
    }

    @Bean
    JwtDecoder jwtDecoder() {
        return mock(JwtDecoder.class);
    }

    @Bean
    OAuth2AuthorizedClientService authorizedClientService(ClientRegistrationRepository clientRegistrationRepository) {
        return new InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository);
    }
}
