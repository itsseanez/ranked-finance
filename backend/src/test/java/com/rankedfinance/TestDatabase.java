package com.rankedfinance;

import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Empties every app table between tests. MockMvc requests commit, so tests that write call this
 * from {@code @AfterEach}. It finds the tables itself, so a table added in a later migration can't
 * be forgotten and leave rows behind for the next test.
 */
public final class TestDatabase {

    private TestDatabase() {}

    public static void deleteAllRows(JdbcClient jdbcClient) {
        // Table names can't be bind parameters; format('%I') quotes each one as an identifier.
        String tables =
                jdbcClient
                        .sql(
                                """
                                select string_agg(format('%I', tablename), ', ')
                                from pg_tables
                                where schemaname = current_schema()
                                  and tablename <> 'flyway_schema_history'
                                """)
                        .query(String.class)
                        .single();
        jdbcClient.sql("truncate table " + tables + " cascade").update();
    }
}
