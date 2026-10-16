package com.minh.fakebook.gateway.presence;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

@Configuration
public class PresenceRedisConfiguration {
    @Bean
    static BeanPostProcessor initializeReactiveRedisBeforeServingRequests() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessBeforeInitialization(Object bean, String name) {
                if (bean instanceof LettuceConnectionFactory factory) {
                    // Lazy shared-connection creation performs a blocking connect even
                    // through ReactiveRedisTemplate. Do it on the startup thread.
                    factory.setEagerInitialization(true);
                }
                return bean;
            }
        };
    }
}
