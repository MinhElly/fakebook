package com.minh.fakebook.media;

import com.minh.fakebook.media.config.AsyncSyncConfiguration;
import com.minh.fakebook.media.config.DatabaseTestcontainer;
import com.minh.fakebook.media.config.TestSecurityConfiguration;
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
        MediaServiceApp.class,
        AsyncSyncConfiguration.class,
        TestSecurityConfiguration.class,
        com.minh.fakebook.media.config.JacksonHibernateConfiguration.class,
        DatabaseTestcontainer.class,
    }
)
public @interface IntegrationTest {}
