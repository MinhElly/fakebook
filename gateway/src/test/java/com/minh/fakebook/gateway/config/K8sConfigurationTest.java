package com.minh.fakebook.gateway.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.FileSystemResource;

class K8sConfigurationTest {

    @Test
    void loadsProductionK8sConfigurationWithoutConsulAndResolvesExternalSettings() throws Exception {
        StandardEnvironment environment = new StandardEnvironment();
        Map<String, Object> settings = new HashMap<>();
        settings.put("spring.config.location", "file:src/main/resources/config/application.yml");
        settings.put("spring.profiles.active", "prod,k8s");
        settings.put("KAFKA_BOOTSTRAP_SERVERS", "broker.example:9093");
        settings.put("REDIS_HOST", "redis.example");
        settings.put("REDIS_URL", "redis://redis.example:6379");
        settings.put("FAKEBOOK_OIDC_ISSUER_URI", "https://auth.example/realms/fakebook");
        settings.put("FAKEBOOK_OIDC_CLIENT_ID", "backend");
        settings.put("FAKEBOOK_OIDC_CLIENT_SECRET", "backend-secret");
        settings.put("FAKEBOOK_OIDC_GATEWAY_CLIENT_ID", "gateway");
        settings.put("FAKEBOOK_OIDC_GATEWAY_CLIENT_SECRET", "gateway-secret");
        settings.put("ZIPKIN_ENDPOINT", "http://tracing.example:9411/api/v2/spans");
        settings.put("SPRING_DATASOURCE_URL", "jdbc:mariadb://db.example:3306/service");
        settings.put("SPRING_R2DBC_URL", "r2dbc:mariadb://db.example:3306/gateway");
        settings.put("SPRING_LIQUIBASE_URL", "jdbc:mariadb://db.example:3306/gateway");
        settings.put("FAKEBOOK_DB_USERNAME", "test-user");
        settings.put("FAKEBOOK_DB_PASSWORD", "test-password");
        settings.put("FRONTEND_ORIGIN", "https://frontend.example");
        settings.put("CLOUDINARY_CLOUD_NAME", "test-cloud");
        settings.put("CLOUDINARY_API_KEY", "test-key");
        settings.put("CLOUDINARY_API_SECRET", "cloudinary-secret");
        environment.getPropertySources().addFirst(new MapPropertySource("k8s-test-settings", settings));

        new YamlPropertySourceLoader()
            .load("k8s-bootstrap", new FileSystemResource("src/main/resources/config/bootstrap-k8s.yml"))
            .forEach(source -> {
                assertThat(source.getProperty("spring.cloud.consul.enabled")).isEqualTo(false);
                assertThat(source.getProperty("spring.cloud.consul.config.enabled")).isEqualTo(false);
                assertThat(source.getProperty("spring.cloud.consul.discovery.enabled")).isEqualTo(false);
                environment.getPropertySources().addLast(source);
            });

        ConfigDataEnvironmentPostProcessor.applyTo(environment);
        assertThat(environment.getProperty("server.port", Integer.class)).isEqualTo(8080);
        assertThat(environment.getProperty("spring.cloud.consul.enabled", Boolean.class)).isFalse();
        assertThat(environment.getProperty("spring.cloud.consul.config.enabled", Boolean.class)).isFalse();
        assertThat(environment.getProperty("spring.cloud.consul.discovery.enabled", Boolean.class)).isFalse();
        assertThat(environment.getProperty("spring.kafka.bootstrap-servers")).isEqualTo("broker.example:9093");
        assertThat(environment.getProperty("spring.cloud.stream.kafka.binder.brokers")).isEqualTo("broker.example:9093");
        assertThat(environment.getProperty("management.tracing.export.zipkin.endpoint")).isEqualTo("http://tracing.example:9411/api/v2/spans");
        assertThat(environment.getProperty("jhipster.cache.redis.server")).isEqualTo("redis://redis.example:6379");

        assertThat(environment.getProperty("spring.cloud.gateway.server.webflux.discovery.locator.enabled", Boolean.class)).isFalse();
        assertThat(environment.getProperty("spring.cloud.gateway.server.webflux.httpclient.response-timeout")).isEqualTo("10s");
        assertThat(environment.getProperty("spring.cloud.gateway.server.webflux.routes[2].metadata.response-timeout", Integer.class))
            .isEqualTo(60000);
        assertThat(environment.getProperty("resilience4j.timelimiter.configs.default.timeout-duration")).isEqualTo("12s");
        assertThat(environment.getProperty("resilience4j.timelimiter.instances.mediaservice.timeout-duration")).isEqualTo("65s");
        for (int index = 0; index < 5; index++) {
            String prefix = "spring.cloud.gateway.server.webflux.routes[" + index + "]";
            assertThat(environment.getProperty(prefix + ".uri")).startsWith("http://");
            assertThat(environment.getProperty(prefix + ".predicates[0]")).startsWith("Path=/services/");
            assertThat(environment.getProperty(prefix + ".filters[0]")).isEqualTo("StripPrefix=2");
            assertThat(environment.getProperty(prefix + ".filters[1].name")).isEqualTo("CircuitBreaker");
        }
        assertThat(environment.getProperty("spring.security.oauth2.client.registration.oidc.client-secret")).isEqualTo("gateway-secret");
    }
}

