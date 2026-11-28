package com.rankedfinance.accounts;

import jakarta.validation.constraints.NotNull;

public record BalanceRequest(@NotNull String balance) {}
