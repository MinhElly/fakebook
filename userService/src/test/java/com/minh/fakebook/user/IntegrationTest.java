package com.minh.fakebook.user;

import com.minh.fakebook.user.config.AsyncSyncConfiguration;
import com.minh.fakebook.user.config.TestSecurityConfiguration;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;
import com.minh.fakebook.user.config.MockCacheConfiguration;

/**
 * Base composite annotation for integration tests.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(
    classes = {
        UserServiceApp.class,
        AsyncSyncConfiguration.class,
        TestSecurityConfiguration.class,
        com.minh.fakebook.user.config.JacksonHibernateConfiguration.class,
        TestSecurityConfiguration.class,
        com.minh.fakebook.user.config.JacksonHibernateConfiguration.class,
        MockCacheConfiguration.class,
    }
)
public @interface IntegrationTest {}
