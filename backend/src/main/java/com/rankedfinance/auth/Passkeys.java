package com.rankedfinance.auth;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Questions about registered passkeys that Spring Security's own repositories don't answer. */
@Repository
class Passkeys {

    private final JdbcClient jdbcClient;

    Passkeys(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    /** Whether any passkey exists on this install. Once one does, first-run setup is closed. */
    long count() {
        return jdbcClient.sql("select count(*) from user_credentials").query(Long.class).single();
    }

    boolean anyRegistered() {
        return jdbcClient
                .sql("select exists (select 1 from user_credentials)")
                .query(Boolean.class)
                .single();
    }
}
