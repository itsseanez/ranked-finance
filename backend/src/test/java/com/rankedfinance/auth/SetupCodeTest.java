package com.rankedfinance.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

// The first-run setup code (ADR 0012): the operator's SETUP_CODE when set, otherwise one generated
// at
// startup and printed to the log until the first passkey exists.
class SetupCodeTest {

    private static final String CONFIGURED = "operator-chosen-code-0123";

    private static SetupCode configured() {
        return new SetupCode(new SetupProperties(CONFIGURED));
    }

    private static SetupCode generated() {
        return new SetupCode(new SetupProperties(""));
    }

    @Test
    void usesTheConfiguredCode() {
        assertThat(configured().matches(CONFIGURED)).isTrue();
    }

    @Test
    void generatesALongRandomCodeWhenNoneIsConfigured() {
        SetupCode code = generated();

        assertThat(code.value()).hasSizeGreaterThanOrEqualTo(32).matches("[A-Za-z0-9_-]+");
        assertThat(code.matches(code.value())).isTrue();
    }

    @Test
    void treatsAMissingSettingAsNotConfigured() {
        assertThat(new SetupCode(new SetupProperties(null)).value())
                .hasSizeGreaterThanOrEqualTo(32);
    }

    @Test
    void generatesADifferentCodeEachTime() {
        assertThat(generated().value()).isNotEqualTo(generated().value());
    }

    @Test
    void wrongCodeIsRejected() {
        assertThat(generated().matches("not-the-setup-code")).isFalse();
    }

    // The log indents the code, so a drag-selected copy often brings spaces or a newline with it.
    @Test
    void acceptsACodeWithSurroundingWhitespace() {
        SetupCode code = generated();

        assertThat(code.matches("    " + code.value() + "\n")).isTrue();
    }

    @Test
    void whitespaceOnlyNeverMatches() {
        assertThat(generated().matches("   ")).isFalse();
    }

    @Test
    void emptyCodeNeverMatches() {
        for (SetupCode code : List.of(generated(), configured())) {
            assertThat(code.matches("")).isFalse();
            assertThat(code.matches(null)).isFalse();
        }
    }

    @Test
    void printsTheGeneratedCodeWhileSetupIsOpen() {
        SetupCode code = generated();

        assertThat(code.startupMessage(false))
                .hasValueSatisfying(message -> assertThat(message).contains(code.value()));
    }

    @Test
    void printsNothingOnceAPasskeyExists() {
        assertThat(generated().startupMessage(true)).isEmpty();
    }

    // The operator already knows a code they chose, and it must never end up in the logs.
    @Test
    void neverPrintsAConfiguredCode() {
        assertThat(configured().startupMessage(false)).isEmpty();
    }
}
