package com.minh.fakebook.gateway.config;

import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;

@TestConfiguration(proxyBeanMethods = false)
public class DatabaseTestcontainer {

    private static final MariaDBContainer<?> DATABASE_CONTAINER = (MariaDBContainer) new MariaDBContainer<>("mariadb:12.3.3")
        .withDatabaseName("gateway")

        .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(DatabaseTestcontainer.class)))
        .withReuse(true);

    @Bean
    @ServiceConnection
    MariaDBContainer<?> databaseContainer() {
        return DATABASE_CONTAINER;
    }

    @Bean
    DynamicPropertyRegistrar databaseProperties(MariaDBContainer<?> databaseContainer) {
        return registry -> {
            registry.add("spring.liquibase.url", () -> databaseContainer.getJdbcUrl() + "?useLegacyDatetimeCode=false");
            registry.add("spring.liquibase.user", databaseContainer::getUsername);
            registry.add("spring.liquibase.password", databaseContainer::getPassword);
        };
    }
}
