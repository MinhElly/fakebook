package com.minh.fakebook.gateway.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.gateway.filter.factory.SpringCloudCircuitBreakerFilterFactory;
import org.springframework.expression.spel.standard.SpelExpressionParser;

class GatewayCircuitBreakerConfigurationTest {

    private static final String GATEWAY_PREFIX = "spring.cloud.gateway.server.webflux";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withInitializer(new ConfigDataApplicationContextInitializer())
        .withPropertyValues(
            "spring.config.location=file:src/main/resources/config/application.yml",
            "spring.profiles.active=dev"
        );

    @Test
    void shouldProtectEachDiscoveryRouteWithItsOwnCircuitBreaker() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();

            var environment = context.getEnvironment();
            assertThat(environment.getProperty(GATEWAY_PREFIX + ".discovery.locator.filters[0].name"))
                .isEqualTo("CircuitBreaker");
            assertThat(environment.getProperty(GATEWAY_PREFIX + ".discovery.locator.filters[0].args.name"))
                .isEqualTo("serviceId");
            assertThat(environment.getProperty(GATEWAY_PREFIX + ".discovery.locator.filters[0].args.statusCodes"))
                .isEqualTo("'500,502,503,504'");
            assertThat(environment.getProperty(GATEWAY_PREFIX + ".discovery.locator.filters[0].args.resumeWithoutError"))
                .isEqualTo("'false'");
            String statusCodesExpression = environment.getProperty(
                GATEWAY_PREFIX + ".discovery.locator.filters[0].args.statusCodes"
            );
            String evaluatedStatusCodes = new SpelExpressionParser().parseExpression(statusCodesExpression).getValue(String.class);
            var filterConfig = new Binder(
                new MapConfigurationPropertySource(Map.of("circuit.statusCodes", evaluatedStatusCodes))
            )
                .bind("circuit", Bindable.of(SpringCloudCircuitBreakerFilterFactory.Config.class))
                .orElseThrow(() -> new IllegalStateException("Gateway status codes cannot be bound"));
            assertThat(filterConfig.getStatusCodes()).containsExactlyInAnyOrder("500", "502", "503", "504");
            assertThat(environment.getProperty(GATEWAY_PREFIX + ".discovery.locator.filters[1]"))
                .isEqualTo("StripPrefix=2");
        });
    }

    @Test
    void shouldConfigureTimeoutsResilienceAndActuatorEvidence() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();

            var environment = context.getEnvironment();
            assertThat(environment.getProperty(GATEWAY_PREFIX + ".httpclient.connect-timeout", Integer.class)).isEqualTo(1500);
            assertThat(
                Binder.get(environment)
                    .bind(GATEWAY_PREFIX + ".httpclient.response-timeout", Duration.class)
                    .orElseThrow(() -> new IllegalStateException("Gateway response timeout configuration is missing"))
            )
                .isEqualTo(Duration.ofSeconds(3));
            assertThat(environment.getProperty("resilience4j.circuitbreaker.configs.default.sliding-window-size", Integer.class))
                .isEqualTo(5);
            assertThat(
                Binder.get(environment)
                    .bind("resilience4j.timelimiter.configs.default.timeout-duration", Duration.class)
                    .orElseThrow(() -> new IllegalStateException("Circuit breaker timeout configuration is missing"))
            )
                .isEqualTo(Duration.ofSeconds(4));

            List<String> exposedEndpoints = Binder.get(environment)
                .bind("management.endpoints.web.exposure.include", Bindable.listOf(String.class))
                .orElseThrow(() -> new IllegalStateException("Management endpoint exposure configuration is missing"));
            assertThat(exposedEndpoints).contains("gateway", "metrics", "circuitbreakers", "circuitbreakerevents");
        });
    }
}
