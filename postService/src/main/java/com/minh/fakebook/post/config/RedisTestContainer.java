package com.minh.fakebook.post.config;

import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class RedisTestContainer {

    private static final GenericContainer<?> REDIS_CONTAINER = new GenericContainer<>(DockerImageName.parse("redis:7.2.4"))
        .withExposedPorts(6379)
        .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(RedisTestContainer.class)))
        .withReuse(true);

    @Bean
    @ServiceConnection(name = "redis")
    GenericContainer<?> redisContainer() {
        return REDIS_CONTAINER;
    }
}
