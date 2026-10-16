package com.minh.fakebook.user.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.minh.fakebook.user.service.dto.UserProfileCacheDTO;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.transaction.TransactionAwareCacheDecorator;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;

/** Exercises the actual cache serializer, rather than mocked Cache values. */
class UserProfileCacheSerializationTest {

    private SerializationPair<Object> values;

    @BeforeEach
    void configureSerializer() {
        RedisCacheManager manager = new CacheConfiguration().cacheManager(
            mock(RedisConnectionFactory.class), new ApplicationProperties());
        manager.afterPropertiesSet();
        var cache = (TransactionAwareCacheDecorator) manager.getCache(Constants.USER_PROFILE_CACHE);
        values = ((RedisCache) cache.getTargetCache()).getCacheConfiguration().getValueSerializationPair();
    }

    @Test
    void profileRoundTripDoesNotSerializeComputedEmptyFlag() {
        UserProfileCacheDTO profile = profile();
        String json = json(profile);

        assertThat(json).doesNotContain("\"empty\"");
        assertThat(read(json)).isEqualTo(profile);
        assertThat(((UserProfileCacheDTO) read(json)).isEmpty()).isFalse();
    }

    @Test
    void negativeCacheSentinelSurvivesRoundTripWithoutComputedFlag() {
        String json = json(UserProfileCacheDTO.EMPTY);

        assertThat(json).doesNotContain("\"empty\"");
        assertThat(read(json)).isEqualTo(UserProfileCacheDTO.EMPTY);
        assertThat(((UserProfileCacheDTO) read(json)).isEmpty()).isTrue();
    }

    @Test
    void readsLegacyProfileWithEmptyFlagWithoutTrustingTheFlag() {
        UserProfileCacheDTO profile = profile();

        assertThat(read(withLegacyFlag(profile, false))).isEqualTo(profile);
        UserProfileCacheDTO restored = (UserProfileCacheDTO) read(withLegacyFlag(profile, true));
        assertThat(restored).isEqualTo(profile);
        assertThat(restored.isEmpty()).isFalse();
    }

    @Test
    void readsLegacyNegativeCacheSentinelWithoutTrustingTheFlag() {
        assertThat(read(withLegacyFlag(UserProfileCacheDTO.EMPTY, true))).isEqualTo(UserProfileCacheDTO.EMPTY);
        UserProfileCacheDTO restored = (UserProfileCacheDTO) read(withLegacyFlag(UserProfileCacheDTO.EMPTY, false));
        assertThat(restored).isEqualTo(UserProfileCacheDTO.EMPTY);
        assertThat(restored.isEmpty()).isTrue();
    }

    private String withLegacyFlag(UserProfileCacheDTO profile, boolean empty) {
        String json = json(profile);
        // Jackson 2 previously emitted this getter as a property beside the record components.
        return json.substring(0, json.length() - 1) + ",\"empty\":" + empty + "}";
    }

    private String json(Object value) {
        return StandardCharsets.UTF_8.decode(values.write(value)).toString();
    }

    private Object read(String json) {
        return values.read(ByteBuffer.wrap(json.getBytes(StandardCharsets.UTF_8)));
    }

    private UserProfileCacheDTO profile() {
        return new UserProfileCacheDTO(UUID.randomUUID(), "cache-user", "Người dùng", "Tiểu sử", "Hà Nội",
            "University", "Engineer", UUID.randomUUID(), UUID.randomUUID(),
            Instant.parse("2026-10-16T10:00:00Z"), Instant.parse("2026-10-16T11:00:00Z"));
    }
}
