package com.rankedfinance.auth;

import static java.nio.charset.StandardCharsets.UTF_8;

import jakarta.validation.constraints.Pattern;
import java.security.MessageDigest;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * The first-run setup code from SETUP_CODE (ADR 0010). The app refuses to start if it's set but too
 * short.
 */
@ConfigurationProperties("app.setup")
@Validated
public record SetupProperties(
        @Pattern(regexp = "^$|.{20,}", message = "SETUP_CODE must be at least 20 characters")
                String code) {

    boolean isConfigured() {
        return code != null && !code.isBlank();
    }

    /**
     * Constant-time comparison, so response timing doesn't reveal how much of the code was right.
     */
    boolean matches(String submitted) {
        if (!isConfigured() || submitted == null) {
            return false;
        }
        return MessageDigest.isEqual(code.getBytes(UTF_8), submitted.getBytes(UTF_8));
    }
}
