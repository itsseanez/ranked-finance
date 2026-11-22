package com.rankedfinance.user;

import java.util.UUID;

/**
 * One row of the {@code users} table. The About-you fields are null when the user prefers not to
 * say.
 */
public record User(
        UUID id,
        String email,
        String displayName,
        Integer birthYear,
        String sex,
        String maritalStatus,
        String livingSituation,
        String country,
        String timeZone,
        String homeCurrency) {}
