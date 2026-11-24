package com.rankedfinance.auth;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.stereotype.Component;

/**
 * The first-run setup code (ADR 0012): SETUP_CODE when the operator set one, otherwise a random
 * code generated at startup and kept only in memory. FirstRunSetup prints a generated code.
 */
@Component
class SetupCode {

    private static final SecureRandom RANDOM = new SecureRandom();

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

    /** True when SETUP_CODE wasn't set and this code was generated. Only those are printed. */
    boolean isGenerated() {
        return generated;
    }

    private static String randomCode() {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
