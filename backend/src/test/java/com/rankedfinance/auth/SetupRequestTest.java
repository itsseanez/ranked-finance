package com.rankedfinance.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

// Spring MVC logs parsed request bodies at DEBUG, so the body's toString must never contain the
// setup code (CLAUDE.md: secrets never appear in logs).
class SetupRequestTest {

    @Test
    void toStringNeverShowsTheCode() {
        String code = "operator-chosen-code-0123";

        assertThat(new SetupController.SetupRequest(code).toString()).doesNotContain(code);
    }
}
