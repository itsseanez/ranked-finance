package com.rankedfinance.auth;

/**
 * Spring Security role names, in one place so a typo can't quietly turn into a 403. Spring stores a
 * role as the authority "ROLE_" + name: {@code hasRole(OWNER)} checks for "ROLE_OWNER".
 */
public final class Roles {

    /** The signed-in owner: everything in the app. */
    public static final String OWNER = "OWNER";

    /** A setup session: registering the first passkey only (ADR 0013). */
    public static final String SETUP = "SETUP";

    private Roles() {}

    public static String authority(String role) {
        return "ROLE_" + role;
    }
}
