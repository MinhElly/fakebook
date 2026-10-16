package com.minh.fakebook.gateway.client;

import org.springframework.security.oauth2.client.AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class InternalServiceTokenProvider {

    private static final String REGISTRATION_ID = "internal";
    private static final String SERVICE_PRINCIPAL = "gateway-presence";

    private final ReactiveOAuth2AuthorizedClientManager manager;

    public InternalServiceTokenProvider(
        ReactiveClientRegistrationRepository registrations,
        ReactiveOAuth2AuthorizedClientService authorizedClientService
    ) {
        var provider =
            ReactiveOAuth2AuthorizedClientProviderBuilder.builder()
                .clientCredentials()
                .build();

        var serviceManager =
            new AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(
                registrations,
                authorizedClientService
            );

        serviceManager.setAuthorizedClientProvider(provider);

        this.manager = serviceManager;
    }

    public Mono<String> getAccessToken() {
        return Mono.defer(() -> {
                OAuth2AuthorizeRequest request =
                    OAuth2AuthorizeRequest
                        .withClientRegistrationId(REGISTRATION_ID)
                        .principal(SERVICE_PRINCIPAL)
                        .build();

                return manager.authorize(request);
            })
            .switchIfEmpty(Mono.error(
                new IllegalStateException(
                    "Could not acquire internal service token"
                )
            ))
            .map(client ->
                client.getAccessToken().getTokenValue()
            );
    }
}
