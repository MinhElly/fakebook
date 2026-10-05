package com.minh.fakebook.user.config;

import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Configuration;

/** Cache reads and fills are optional; database reads remain available during Redis outages. */
@org.springframework.transaction.annotation.EnableTransactionManagement(order = 0)
@Configuration
public class CacheFailureConfiguration implements CachingConfigurer {
    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            private void report(RuntimeException error, Cache cache) {
                LoggerFactory.getLogger(CacheFailureConfiguration.class).warn("Cache {} unavailable; using database", cache.getName(), error);
            }
            @Override public void handleCacheGetError(RuntimeException e, Cache cache, Object key) { report(e, cache); }
            @Override public void handleCachePutError(RuntimeException e, Cache cache, Object key, Object value) { report(e, cache); }
            @Override public void handleCacheEvictError(RuntimeException e, Cache cache, Object key) { throw e; }
            @Override public void handleCacheClearError(RuntimeException e, Cache cache) { throw e; }
        };
    }
}
