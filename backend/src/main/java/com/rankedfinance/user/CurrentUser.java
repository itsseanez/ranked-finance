package com.rankedfinance.user;

import java.util.UUID;

/**
 * The signed-in user, as a controller parameter. Scope every query to {@code id}. Filled in by
 * CurrentUserArgumentResolver.
 */
public record CurrentUser(UUID id) {}
