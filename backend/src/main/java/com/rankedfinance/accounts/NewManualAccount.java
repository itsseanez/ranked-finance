package com.rankedfinance.accounts;

import java.math.BigDecimal;

public record NewManualAccount(
        String name,
        String institutionName,
        AccountKind kind,
        String currency,
        BigDecimal apr,
        BigDecimal minimumPayment,
        UpdateReminder updateReminder) {}
