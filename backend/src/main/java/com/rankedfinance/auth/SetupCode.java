package com.rankedfinance.auth;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * The first-run setup code (ADR 0012): SETUP_CODE when the operator set one, otherwise a random
 * code generated at startup and kept only in memory. Only a generated code is ever printed.
 */
@Component
class SetupCode {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String STARS = "*".repeat(64);

    private final String value;
    private final boolean generated;

    SetupCode(SetupProperties properties) {
        String configured = properties.code();
        this.generated = configured == null || configured.isBlank();
        this.value = generated ? randomCode() : configured;
    }

    String value() {
        return value;
    }

    /**
     * Constant-time comparison, so response timing doesn't reveal how much of the code was right.
     * Surrounding whitespace is ignored: the log indents the code, so a copied code often has some.
     */
    boolean matches(String submitted) {
        if (submitted == null || submitted.isBlank()) {
            return false;
        }
        return MessageDigest.isEqual(value.getBytes(UTF_8), submitted.strip().getBytes(UTF_8));
    }

    /**
     * What to print at startup: the generated code while setup is open, and nothing otherwise. A
     * code the operator chose is never printed.
     */
    Optional<String> startupMessage(boolean passkeyRegistered) {
        if (!generated || passkeyRegistered) {
            return Optional.empty();
        }
        return Optional.of(
                """

                %s
                Ranked Finance setup code:

                    %s

                Open the app, choose "First-time setup", and enter this code to create
                your passkey. It stops working once a passkey exists. While setup is
                open, the server prints a new code each time it starts.
                To choose your own code instead, set SETUP_CODE.
                %s"""
                        .formatted(STARS, value, STARS));
    }

    private static String randomCode() {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
