package com.minh.fakebook.gateway.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.minh.fakebook.gateway.presence.PresenceRedisConfiguration;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration;
import org.springframework.boot.data.redis.autoconfigure.DataRedisReactiveAutoConfiguration;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;

@ExtendWith(RedisTestContainer.class)
class PresenceRedisConnectionIT {

    private static final Duration WAIT =
        Duration.ofSeconds(5);

    private static final Duration KEY_TTL =
        Duration.ofSeconds(60);

    private final ApplicationContextRunner contextRunner =
        new ApplicationContextRunner()
            .withInitializer(
                new ConfigDataApplicationContextInitializer()
            )
            .withInitializer(new RedisTestContainer())
            .withUserConfiguration(PresenceRedisConfiguration.class)
            .withPropertyValues(
                "spring.config.location=file:src/main/resources/config/application.yml",
                "spring.profiles.active=presence-redis-test",
                "REDIS_URL=redis://unused.example:1"
            )
            .withConfiguration(
                AutoConfigurations.of(
                    DataRedisAutoConfiguration.class,
                    DataRedisReactiveAutoConfiguration.class
                )
            );

    @Test
    void shouldConnectAndWriteReadDeleteTemporaryKey() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context)
                .hasSingleBean(ReactiveStringRedisTemplate.class);

            ReactiveStringRedisTemplate redis =
                context.getBean(ReactiveStringRedisTemplate.class);

            String pong = redis
                .execute(connection -> connection.ping())
                .single()
                .block(WAIT);

            assertThat(pong).isEqualTo("PONG");

            String key =
                "fakebook:presence:test:" + UUID.randomUUID();

            Boolean written = redis.opsForValue()
                .set(key, "connected", KEY_TTL)
                .block(WAIT);

            assertThat(written).isTrue();

            try {
                String value = redis.opsForValue()
                    .get(key)
                    .block(WAIT);

                assertThat(value).isEqualTo("connected");

                Duration remaining = redis
                    .getExpire(key)
                    .block(WAIT);

                assertThat(remaining)
                    .isNotNull()
                    .isGreaterThan(Duration.ZERO)
                    .isLessThanOrEqualTo(KEY_TTL);
            } finally {
                redis.delete(key).block(WAIT);
            }

            assertThat(redis.hasKey(key).block(WAIT))
                .isFalse();
        });
    }
}
