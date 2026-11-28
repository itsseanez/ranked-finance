package com.rankedfinance.user;

import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class UserRepository {

    private final JdbcClient jdbcClient;

    public UserRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    /** Creates the install's single owner. v1 allows exactly one user, so a second call fails. */
    @Transactional
    public User createOwner(String email, String displayName) {
        if (count() > 0) {
            throw new IllegalStateException("owner already exists");
        }
        return jdbcClient
                .sql(
                        """
                        insert into users (id, email, display_name)
                        values (:id, :email, :displayName)
                        returning id, email, display_name, birth_year, sex, marital_status,
                                  living_situation, country, time_zone, home_currency
                        """)
                .param("id", UUID.randomUUID())
                .param("email", email)
                .param("displayName", displayName)
                .query(User.class)
                .single();
    }

    /**
     * Creates the owner unless one already exists, then returns the owner with this email. The
     * insert is one statement, so two setups at once can't race. Empty when the install's owner has
     * a different email (OWNER_EMAIL changed after setup started).
     */
    @Transactional
    public Optional<User> findOrCreateOwner(String email, String displayName) {
        jdbcClient
                .sql(
                        """
                        insert into users (id, email, display_name)
                        values (:id, :email, :displayName)
                        on conflict do nothing
                        """)
                .param("id", UUID.randomUUID())
                .param("email", email)
                .param("displayName", displayName)
                .update();
        return findByEmail(email);
    }

    /** Finds a user by sign-in email. Passkey login uses it to load the signed-in user. */
    public Optional<User> findByEmail(String email) {
        return jdbcClient
                .sql(
                        """
                        select id, email, display_name, birth_year, sex, marital_status,
                               living_situation, country, time_zone, home_currency
                        from users
                        where email = :email
                        """)
                .param("email", email)
                .query(User.class)
                .optional();
    }

    /** Finds a user by id: the signed-in user's id from {@link CurrentUser}. */
    public Optional<User> findById(UUID id) {
        return jdbcClient
                .sql(
                        """
                        select id, email, display_name, birth_year, sex, marital_status,
                               living_situation, country, time_zone, home_currency
                        from users
                        where id = :id
                        """)
                .param("id", id)
                .query(User.class)
                .optional();
    }

    /**
     * Counts every user on the install. Deliberately not scoped to one user: it guards the
     * single-owner rule.
     */
    public long count() {
        return jdbcClient.sql("select count(*) from users").query(Long.class).single();
    }
}
