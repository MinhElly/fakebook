package com.minh.fakebook.user.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

class CacheBehaviorTest {
    @Test void preservesEverySortAttribute() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addSerializer(PageRequest.class, new CachedPageRequestSerializer());
        module.addDeserializer(PageRequest.class, new CacheConfiguration.PageRequestDeserializer());
        mapper.registerModule(module);
        PageRequest request = PageRequest.of(2, 10, Sort.by(
            Sort.Order.desc("displayName").ignoreCase().nullsLast(), Sort.Order.asc("id")));
        assertThat(mapper.readValue(mapper.writeValueAsString(request), PageRequest.class)).isEqualTo(request);
    }
    @Test void cacheFailureAllowsReadAndFill() {
        var handler = new CacheFailureConfiguration().errorHandler();
        Cache cache = mock(Cache.class);
        when(cache.getName()).thenReturn("userFriends");
        handler.handleCacheGetError(new IllegalStateException("Redis down"), cache, "key");
        handler.handleCachePutError(new IllegalStateException("Redis down"), cache, "key", "value");
    }
}
