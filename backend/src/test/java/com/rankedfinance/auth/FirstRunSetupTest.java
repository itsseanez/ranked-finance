package com.rankedfinance.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.rankedfinance.TestcontainersConfiguration;
import com.rankedfinance.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.web.webauthn.management.JdbcUserCredentialRepository;

// First-run setup's rules (ADR 0010, 0012, 0013), tested through the module against real Postgres.
// The HTTP side (sessions, CSRF, status codes) is in SetupTest.
@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class FirstRunSetupTest {

    private static final String CODE = "test-setup-code-0123456789";

    @Autowired JdbcClient jdbcClient;
    @Autowired JdbcOperations jdbcOperations;

    @Test
    void opensSetupWithTheRightCode() {
        assertThat(setup().start(CODE)).isEqualTo(new FirstRunSetup.Opened("owner@example.com"));
        assertThat(userCount()).isEqualTo(1);
    }

    @Test
    void rejectsAWrongCode() {
        assertThat(setup().start("not-the-setup-code")).isEqualTo(new FirstRunSetup.WrongCode());
        assertThat(userCount()).isZero();
    }

    @Test
    void isClosedOnceAPasskeyExists() {
        addPasskey();

        assertThat(setup().isOpen()).isFalse();
        assertThat(setup().start(CODE)).isEqualTo(new FirstRunSetup.Closed());
    }

    // Retrying setup (for example after a cancelled passkey prompt) reuses the owner row.
    @Test
    void canBeRepeatedBeforeAPasskeyExists() {
        FirstRunSetup setup = setup();

        assertThat(setup.start(CODE)).isInstanceOf(FirstRunSetup.Opened.class);
        assertThat(setup.start(CODE)).isInstanceOf(FirstRunSetup.Opened.class);
        assertThat(userCount()).isEqualTo(1);
    }

    @Test
    void reportsAnOwnerMismatch() {
        new UserRepository(jdbcClient).createOwner("someone-else@example.com", "Someone");

        assertThat(setup().start(CODE)).isEqualTo(new FirstRunSetup.OwnerMismatch());
    }

    // Deleting the last passkey would reopen setup to anyone with the code.
    @Test
    void mayRemoveAPasskeyOnlyWhenAnotherRemains() {
        addPasskey();
        assertThat(setup().mayRemovePasskey()).isFalse();

        addPasskey();
        assertThat(setup().mayRemovePasskey()).isTrue();
    }

    @Test
    void announcesTheGeneratedCodeWhileSetupIsOpen() {
        SetupCode code = new SetupCode(new SetupProperties(""));

        assertThat(setup(code).startupMessage())
                .hasValueSatisfying(message -> assertThat(message).contains(code.value()));
    }

    @Test
    void announcesNothingOnceAPasskeyExists() {
        addPasskey();

        assertThat(setup(new SetupCode(new SetupProperties(""))).startupMessage()).isEmpty();
    }

    // The operator already knows a code they chose, and it must never end up in the logs.
    @Test
    void neverAnnouncesAConfiguredCode() {
        assertThat(setup().startupMessage()).isEmpty();
    }

    private FirstRunSetup setup() {
        return setup(new SetupCode(new SetupProperties(CODE)));
    }

    private FirstRunSetup setup(SetupCode code) {
        return new FirstRunSetup(
                code,
                new Passkeys(jdbcClient),
                new UserRepository(jdbcClient),
                new OwnerProperties("owner@example.com", "Owner"));
    }

    private void addPasskey() {
        new JdbcUserCredentialRepository(jdbcOperations).save(TestPasskeys.aPasskey());
    }

    private long userCount() {
        return jdbcClient.sql("select count(*) from users").query(Long.class).single();
    }
}
