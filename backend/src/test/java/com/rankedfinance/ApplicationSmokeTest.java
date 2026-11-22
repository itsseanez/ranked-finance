package com.rankedfinance;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ApplicationSmokeTest {

    @Autowired JdbcClient jdbcClient;

    @Test
    void contextLoadsAndUsersTableExists() {
        Long count = jdbcClient.sql("select count(*) from users").query(Long.class).single();
        assertThat(count).isEqualTo(0L);
    }
}
