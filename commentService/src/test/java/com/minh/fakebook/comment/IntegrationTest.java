package com.minh.fakebook.comment;

import com.minh.fakebook.comment.config.AsyncSyncConfiguration;
import com.minh.fakebook.comment.config.DatabaseTestcontainer;
import com.minh.fakebook.comment.config.TestSecurityConfiguration;
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
        CommentServiceApp.class,
        AsyncSyncConfiguration.class,
        TestSecurityConfiguration.class,
        com.minh.fakebook.comment.config.JacksonHibernateConfiguration.class,
        DatabaseTestcontainer.class,
    }
)
public @interface IntegrationTest {}
