package com.rankedfinance.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Who the owner is, from OWNER_EMAIL and OWNER_DISPLAY_NAME. Used when first-run setup creates the
 * owner.
 */
@ConfigurationProperties("app.owner")
public record OwnerProperties(String email, String displayName) {}
