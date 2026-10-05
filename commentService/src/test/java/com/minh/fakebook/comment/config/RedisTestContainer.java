package com.minh.fakebook.comment.config;

import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;

public class RedisTestContainer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final GenericContainer REDIS_CONTAINER = new GenericContainer("redis:8.10.1")
        .withExposedPorts(6379)
        .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(RedisTestContainer.class)))
        .withReuse(true);

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        REDIS_CONTAINER.start();
        String redisUrl = "redis://" + REDIS_CONTAINER.getContainerIpAddress() + ":" + REDIS_CONTAINER.getMappedPort(6379);
        TestPropertySourceUtils.addInlinedPropertiesToEnvironment(applicationContext, "jhipster.cache.redis.server=" + redisUrl);
    }
}