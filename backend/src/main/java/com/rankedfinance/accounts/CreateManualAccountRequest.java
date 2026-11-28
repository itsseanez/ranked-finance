package com.rankedfinance.accounts;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateManualAccountRequest(
        @NotBlank String name,
        String institutionName,
        @NotNull String kind,
        @NotNull String balance,
        String apr,
        String minimumPayment,
        String updateReminder) {}
