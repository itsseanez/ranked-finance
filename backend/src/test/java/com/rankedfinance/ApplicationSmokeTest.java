package com.rankedfinance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.BadSqlGrammarException;
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

    // The app connects as a role that reads and writes rows only; Flyway migrates as the owner.
    @Test
    void appRunsAsTheRestrictedUser() {
        String user = jdbcClient.sql("select current_user").query(String.class).single();
        assertThat(user).isEqualTo("rankedfinance_app");
    }

    @Test
    void appUserCannotChangeTheSchema() {
        assertThatThrownBy(() -> jdbcClient.sql("create table x (id int)").update())
                .isInstanceOf(BadSqlGrammarException.class)
                .rootCause()
                .hasMessageContaining("permission denied");
    }

    @Test
    void appUserCannotEditMigrationHistory() {
        assertThatThrownBy(() -> jdbcClient.sql("delete from flyway_schema_history").update())
                .isInstanceOf(DataAccessException.class)
                .rootCause()
                .hasMessageContaining("permission denied");
    }
}
