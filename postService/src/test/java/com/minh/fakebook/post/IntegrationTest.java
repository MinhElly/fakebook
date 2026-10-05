package com.minh.fakebook.post;

import com.minh.fakebook.post.config.AsyncSyncConfiguration;
import com.minh.fakebook.post.config.DatabaseTestcontainer;
import com.minh.fakebook.post.config.JacksonHibernateConfiguration;
import com.minh.fakebook.post.config.TestSecurityConfiguration;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;
import com.minh.fakebook.post.config.RedisTestContainer;

/**
 * Base composite annotation for integration tests.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@org.springframework.test.context.ContextConfiguration(initializers = RedisTestContainer.class)
@SpringBootTest(
    classes = {
        PostServiceApp.class,
        AsyncSyncConfiguration.class,
        TestSecurityConfiguration.class,
        JacksonHibernateConfiguration.class,
        DatabaseTestcontainer.class
    }
)
public @interface IntegrationTest {}
