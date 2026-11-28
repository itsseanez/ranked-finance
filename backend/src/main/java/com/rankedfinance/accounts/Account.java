package com.rankedfinance.accounts;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * An account with its latest snapshot. balance and balanceAsOf are null until one exists.
 * snapshotCount is capped at 2 (0, 1, or "2 or more"), which is all delete needs to know.
 */
public record Account(
        UUID id,
        AccountSource source,
        String name,
        String institutionName,
        String last4,
        AccountKind kind,
        String currency,
        BigDecimal apr,
        BigDecimal minimumPayment,
        UpdateReminder updateReminder,
        boolean hidden,
        LocalDate closedOn,
        BigDecimal balance,
        LocalDate balanceAsOf,
        int snapshotCount) {}
