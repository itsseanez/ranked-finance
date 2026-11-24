package com.rankedfinance.auth;

import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * SETUP_CODE, the operator's own first-run setup code (ADR 0010). Optional: without it the server
 * generates one (see SetupCode, ADR 0012). The app refuses to start if it's set but too short.
 */
@ConfigurationProperties("app.setup")
@Validated
public record SetupProperties(
        @Pattern(regexp = "^$|.{20,}", message = "SETUP_CODE must be at least 20 characters")
                String code) {}
