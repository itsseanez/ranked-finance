package com.rankedfinance.config;

import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Where passkeys work: the relying party ID (a domain) and the browser origins allowed to use it.
 */
@ConfigurationProperties("app.webauthn")
public record WebAuthnProperties(String rpId, Set<String> origins) {}
