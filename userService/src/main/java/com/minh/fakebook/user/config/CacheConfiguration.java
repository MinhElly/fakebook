package com.minh.fakebook.user.config;

import java.time.Duration;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@Configuration
@EnableCaching 
public class CacheConfiguration {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PageImplMixin<T> {
        @JsonCreator
        public PageImplMixin(
            @JsonProperty("content") List<T> content,
            @JsonProperty("pageable") Pageable pageable,
            @JsonProperty("totalElements") long totalElements
        ) {}
    }

    public static class PageableDeserializer extends JsonDeserializer<Pageable> {
        @Override
        public Pageable deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            JsonNode node = p.getCodec().readTree(p);
            int page = node.has("pageNumber") ? node.get("pageNumber").asInt() : (node.has("number") ? node.get("number").asInt() : 0);
            int size = node.has("pageSize") ? node.get("pageSize").asInt() : (node.has("size") ? node.get("size").asInt() : 20);
            return PageRequest.of(page, size);
        }
    }

    public static class PageRequestDeserializer extends JsonDeserializer<PageRequest> {
        @Override
        public PageRequest deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            JsonNode node = p.getCodec().readTree(p);
            int page = node.has("pageNumber") ? node.get("pageNumber").asInt() : (node.has("number") ? node.get("number").asInt() : 0);
            int size = node.has("pageSize") ? node.get("pageSize").asInt() : (node.has("size") ? node.get("size").asInt() : 20);
            return PageRequest.of(page, size);
        }
    }

    @Bean 
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory){
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.addMixIn(PageImpl.class, PageImplMixin.class);

        SimpleModule pageModule = new SimpleModule();
        pageModule.addDeserializer(Pageable.class, new PageableDeserializer());
        pageModule.addDeserializer(PageRequest.class, new PageRequestDeserializer());
        objectMapper.registerModule(pageModule);

        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        GenericJackson2JsonRedisSerializer jsonSerializer = GenericJackson2JsonRedisSerializer.builder().objectMapper(objectMapper).build();
        RedisCacheConfiguration cacheConfig = RedisCacheConfiguration.defaultCacheConfig()
        .entryTtl(Duration.ofMinutes(30))
        .disableCachingNullValues()
        .serializeKeysWith(
            RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
        .serializeValuesWith(
            RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer));
        return RedisCacheManager.builder(connectionFactory).cacheDefaults(cacheConfig).build();
    }
}
