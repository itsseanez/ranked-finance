package com.rankedfinance.user;

import java.util.UUID;

/**
 * Who is signed in. The frontend uses it to show the user's name and to detect an expired session.
 */
public record MeResponse(UUID id, String displayName, String email) {}
