package com.rankedfinance.user;

import java.util.UUID;

/**
 * Who is signed in. The frontend uses it to show the user's name, to detect an expired session, and
 * to send the browser's time zone when it differs from {@code timeZone}.
 */
public record MeResponse(UUID id, String displayName, String email, String timeZone) {}
