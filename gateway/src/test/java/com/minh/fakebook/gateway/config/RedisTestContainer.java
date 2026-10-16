package com.minh.fakebook.gateway.config;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

/** One isolated Redis per test JVM, including contexts which eagerly connect at startup. */
public class RedisTestContainer implements ApplicationContextInitializer<ConfigurableApplicationContext>, BeforeAllCallback {

    private static final class RedisHolder {
        private static final GenericContainer<?> CONTAINER = start();

        private static GenericContainer<?> start() {
            GenericContainer<?> container = new GenericContainer<>(DockerImageName.parse("redis:8.10.1"))
                .withExposedPorts(6379);
            container.start();
            return container;
        }
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        // Container discovery/pull belongs to suite setup, outside the 15s method timeout.
        redis();
    }

    private static GenericContainer<?> redis() {
        return RedisHolder.CONTAINER;
    }

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        GenericContainer<?> container = redis();
        String url = "redis://" + container.getHost() + ":" + container.getMappedPort(6379);
        TestPropertySourceUtils.addInlinedPropertiesToEnvironment(context,
            "spring.data.redis.url=" + url,
            "jhipster.cache.redis.server=" + url);
    }
}
