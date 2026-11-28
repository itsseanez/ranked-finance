package com.rankedfinance.accounts;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CloseAccountRequest(@NotNull LocalDate closedOn) {}
