package com.minh.fakebook.comment.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class CircuitBreakerConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withInitializer(new ConfigDataApplicationContextInitializer())
        .withPropertyValues(
            "spring.config.location=file:src/main/resources/config/application.yml",
            "spring.profiles.active=dev"
        );

    @Test
    void shouldLoadOpenFeignCircuitBreakerConfiguration() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();

            var environment = context.getEnvironment();
            assertThat(environment.getProperty("spring.cloud.openfeign.circuitbreaker.enabled", Boolean.class)).isTrue();
            assertThat(environment.getProperty("spring.cloud.openfeign.client.config.default.connectTimeout", Integer.class))
                .isEqualTo(1500);
            assertThat(environment.getProperty("spring.cloud.openfeign.client.config.default.readTimeout", Integer.class)).isEqualTo(3000);
            assertThat(environment.getProperty("resilience4j.circuitbreaker.configs.default.sliding-window-size", Integer.class))
                .isEqualTo(5);
            List<String> ignoredExceptions = Binder.get(environment)
                .bind("resilience4j.circuitbreaker.configs.default.ignore-exceptions", Bindable.listOf(String.class))
                .orElseThrow(() -> new IllegalStateException("Ignored Circuit Breaker exceptions are missing"));
            assertThat(ignoredExceptions).containsExactly("feign.FeignException$FeignClientException");
            Duration timeout = Binder.get(environment)
                .bind("resilience4j.timelimiter.configs.default.timeout-duration", Duration.class)
                .orElseThrow(() -> new IllegalStateException("Time limiter configuration is missing"));
            assertThat(timeout).isEqualTo(Duration.ofSeconds(4));

            List<String> endpoints = Binder.get(environment)
                .bind("management.endpoints.web.exposure.include", Bindable.listOf(String.class))
                .orElseThrow(() -> new IllegalStateException("Management endpoint exposure is missing"));
            assertThat(endpoints).contains("metrics", "circuitbreakers", "circuitbreakerevents");
        });
    }
}
