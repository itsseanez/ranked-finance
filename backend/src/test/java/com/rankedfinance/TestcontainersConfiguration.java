package com.rankedfinance;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    private static final String OWNER = "rankedfinance";
    private static final String APP_ROLE = "rankedfinance_app";

    // Same roles as compose.yaml: the owner is POSTGRES_USER, and the init scripts run as the owner
    // on first start, before Flyway, to create the app role and (tests only) let it TRUNCATE.
    @Bean
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(DockerImageName.parse("postgres:19"))
                .withDatabaseName(OWNER)
                .withUsername(OWNER)
                .withPassword(OWNER)
                .withCopyFileToContainer(
                        MountableFile.forHostPath("../db/init/01-app-role.sql"),
                        "/docker-entrypoint-initdb.d/01-app-role.sql")
                .withCopyFileToContainer(
                        MountableFile.forClasspathResource("db/02-test-truncate.sql"),
                        "/docker-entrypoint-initdb.d/02-test-truncate.sql");
    }

    // A DynamicPropertyRegistrar bean adds Spring properties computed at runtime (here, from the
    // started container), like setting process.env before the app reads its config. It replaces
    // @ServiceConnection, which would connect the app as the container's owner.
    @Bean
    DynamicPropertyRegistrar databaseProperties(PostgreSQLContainer postgres) {
        return registry -> {
            registry.add("spring.datasource.url", postgres::getJdbcUrl);
            registry.add("spring.datasource.username", () -> APP_ROLE);
            registry.add("spring.datasource.password", () -> APP_ROLE);
            registry.add("spring.flyway.user", () -> OWNER);
            registry.add("spring.flyway.password", () -> OWNER);
        };
    }
}
