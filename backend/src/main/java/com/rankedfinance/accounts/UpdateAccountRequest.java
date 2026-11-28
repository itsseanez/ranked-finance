package com.rankedfinance.accounts;

/** Every field is optional; a null one leaves the account's value unchanged. */
public record UpdateAccountRequest(
        String name,
        String institutionName,
        String apr,
        String minimumPayment,
        String updateReminder,
        Boolean hidden) {}
