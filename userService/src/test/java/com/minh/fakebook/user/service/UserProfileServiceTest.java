package com.minh.fakebook.user.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.minh.fakebook.user.IntegrationTest;
import com.minh.fakebook.user.domain.UserProfile;
import com.minh.fakebook.user.repository.UserProfileRepository;
import com.minh.fakebook.user.service.dto.UserSearchDTO;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Transactional
class UserProfileServiceTest {

    @Autowired
    private UserProfileService userProfileService;

    @Autowired
    private UserProfileRepository userProfileRepository;

    private UserProfile user1;
    private UserProfile user2;

    @BeforeEach
    void init() {
        user1 = new UserProfile();
        user1.setId(UUID.randomUUID());
        user1.setUsername("user1");
        user1.setDisplayName("User One");
        user1.setCreatedAt(Instant.now());
        userProfileRepository.saveAndFlush(user1);

        user2 = new UserProfile();
        user2.setId(UUID.randomUUID());
        user2.setUsername("user2");
        user2.setDisplayName("User Two");
        user2.setCreatedAt(Instant.now());
        userProfileRepository.saveAndFlush(user2);
    }

    @Test
    void testUserProfileDtoSerialization() throws Exception {
        Jwt jwt = Jwt.withTokenValue("token")
            .header("alg", "none")
            .claim("sub", user1.getId().toString())
            .claim("preferred_username", user1.getUsername())
            .build();

        com.minh.fakebook.user.service.dto.UserProfileDTO profile = userProfileService.getOrCreateProfile(jwt);

        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator ptv = 
            com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(Object.class)
                .build();
        mapper.activateDefaultTyping(ptv, com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping.NON_FINAL, com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY);

        org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer serializer =
            org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer.builder().objectMapper(mapper).build();

        byte[] serialized = serializer.serialize(profile);
        Object deserialized = serializer.deserialize(serialized);
        assertThat(deserialized).isNotNull();
        assertThat(deserialized).isInstanceOf(com.minh.fakebook.user.service.dto.UserProfileDTO.class);
    }
}
