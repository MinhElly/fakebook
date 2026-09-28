package com.minh.fakebook.comment.config;

import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;

@TestConfiguration(proxyBeanMethods = false)
public class RedisTestContainer {

    private static final GenericContainer REDIS_CONTAINER = new GenericContainer("redis:8.10.1")
        .withExposedPorts(6379)
        .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(RedisTestContainer.class)))
        .withReuse(true);

    static {
        REDIS_CONTAINER.start();
    }

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("jhipster.cache.redis.server", () -> "redis://" + REDIS_CONTAINER.getContainerIpAddress() + ":" + REDIS_CONTAINER.getMappedPort(6379));
    }
}