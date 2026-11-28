package com.rankedfinance.accounts;

import java.time.LocalDate;
import java.util.UUID;

/** An account as the API sends it. Money is a decimal string (ADR 0017), never a JSON number. */
public record AccountResponse(
        UUID id,
        String source,
        String name,
        String institutionName,
        String kind,
        String currency,
        String balance,
        LocalDate balanceAsOf,
        String apr,
        String minimumPayment,
        String updateReminder,
        boolean hidden,
        LocalDate closedOn,
        boolean needsUpdate,
        boolean canDelete) {}
