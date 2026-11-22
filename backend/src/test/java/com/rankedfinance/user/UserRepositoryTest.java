package com.rankedfinance.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rankedfinance.TestcontainersConfiguration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;

// v1 has exactly one user per install: the owner. A second owner would let a stranger in.
@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, UserRepository.class})
class UserRepositoryTest {

    @Autowired UserRepository users;
    @Autowired JdbcClient jdbcClient;

    @Test
    void secondOwnerIsRejected() {
        users.createOwner("owner@example.com", "Owner");

        assertThatThrownBy(() -> users.createOwner("second@example.com", "Second"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("owner already exists");
        assertThat(users.count()).isEqualTo(1);
    }

    @Test
    void findsTheOwnerByEmail() {
        User owner = users.createOwner("owner@example.com", "Owner");

        assertThat(users.findByEmail("owner@example.com")).contains(owner);
        assertThat(users.findByEmail("nobody@example.com")).isEmpty();
    }

    // The Java check above can lose a race (two registrations at once both see zero users),
    // so the database itself must refuse a second row, even when the check is bypassed.
    @Test
    void databaseRejectsASecondUserRow() {
        insertUserRow("owner@example.com");

        assertThatThrownBy(() -> insertUserRow("second@example.com"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private void insertUserRow(String email) {
        jdbcClient
                .sql("insert into users (id, email, display_name) values (:id, :email, :name)")
                .param("id", UUID.randomUUID())
                .param("email", email)
                .param("name", "Owner")
                .update();
    }
}
