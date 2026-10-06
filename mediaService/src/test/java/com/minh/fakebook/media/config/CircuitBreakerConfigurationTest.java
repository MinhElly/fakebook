package com.minh.fakebook.media.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.minh.fakebook.media.service.impl.CloudinaryStorageServiceImpl;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.bind.Bindable;
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
    void shouldLoadCloudinaryCircuitBreakerConfiguration() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();

            var environment = context.getEnvironment();
            assertThat(environment.getProperty("resilience4j.circuitbreaker.instances.cloudinary.base-config"))
                .isEqualTo("default");
            assertThat(environment.getProperty("resilience4j.circuitbreaker.configs.default.sliding-window-size", Integer.class))
                .isEqualTo(5);
            assertThat(environment.getProperty("resilience4j.circuitbreaker.configs.default.minimum-number-of-calls", Integer.class))
                .isEqualTo(5);
            Duration openWait = Binder.get(environment)
                .bind("resilience4j.circuitbreaker.configs.default.wait-duration-in-open-state", Duration.class)
                .orElseThrow(() -> new IllegalStateException("Circuit breaker open wait is missing"));
            assertThat(openWait).isEqualTo(Duration.ofSeconds(10));

            var uploadMethod = CloudinaryStorageServiceImpl.class.getDeclaredMethod(
                "uploadFile",
                org.springframework.web.multipart.MultipartFile.class,
                String.class
            );
            var deleteMethod = CloudinaryStorageServiceImpl.class.getDeclaredMethod("deleteFile", String.class);
            assertThat(uploadMethod.getAnnotation(CircuitBreaker.class).name()).isEqualTo("cloudinary");
            assertThat(deleteMethod.getAnnotation(CircuitBreaker.class).name()).isEqualTo("cloudinary");

            var endpoints = Binder.get(environment)
                .bind("management.endpoints.web.exposure.include", Bindable.listOf(String.class))
                .orElseThrow(() -> new IllegalStateException("Management endpoint exposure is missing"));
            assertThat(endpoints).contains("metrics", "circuitbreakers", "circuitbreakerevents");
        });
    }
}
