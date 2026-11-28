package com.rankedfinance.user;

import java.io.Serializable;
import java.security.Principal;
import java.util.UUID;

/**
 * Who is signed in, as Spring Security stores it. The name stays the email because Spring's passkey
 * registration finds the WebAuthn user entity by it. Serializable because the session stores it.
 */
public record OwnerPrincipal(UUID id, String email) implements Principal, Serializable {

    @Override
    public String getName() {
        return email;
    }
}
