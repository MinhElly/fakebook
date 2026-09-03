package com.minh.fakebook.user;

import com.minh.fakebook.user.config.AsyncSyncConfiguration;
import com.minh.fakebook.user.config.DatabaseTestcontainer;
import com.minh.fakebook.user.config.TestSecurityConfiguration;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;

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
        DatabaseTestcontainer.class,
    }
)
public @interface IntegrationTest {}
