package com.rankedfinance.accounts;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Test builder for Account. Defaults: a manual, open, visible cash account named "Checking". */
public final class AccountBuilder {
    private AccountSource source = AccountSource.MANUAL;
    private String name = "Checking";
    private String institutionName = null;
    private String last4 = null;
    private AccountKind kind = AccountKind.CASH;
    private UpdateReminder reminder = UpdateReminder.MONTHLY;
    private boolean hidden = false;
    private LocalDate closedOn = null;
    private BigDecimal balance = new BigDecimal("100.00");
    private LocalDate balanceAsOf = LocalDate.of(2026, 11, 1);
    private int snapshotCount = 1;

    public static AccountBuilder account() {
        return new AccountBuilder();
    }

    public AccountBuilder source(AccountSource v) {
        source = v;
        return this;
    }

    public AccountBuilder name(String v) {
        name = v;
        return this;
    }

    public AccountBuilder institution(String v) {
        institutionName = v;
        return this;
    }

    public AccountBuilder last4(String v) {
        last4 = v;
        return this;
    }

    public AccountBuilder kind(AccountKind v) {
        kind = v;
        return this;
    }

    public AccountBuilder reminder(UpdateReminder v) {
        reminder = v;
        return this;
    }

    public AccountBuilder hidden() {
        hidden = true;
        return this;
    }

    public AccountBuilder closedOn(LocalDate v) {
        closedOn = v;
        return this;
    }

    public AccountBuilder balanceAsOf(LocalDate v) {
        balanceAsOf = v;
        return this;
    }

    public AccountBuilder noSnapshot() {
        balance = null;
        balanceAsOf = null;
        snapshotCount = 0;
        return this;
    }

    public Account build() {
        return new Account(
                UUID.randomUUID(),
                source,
                name,
                institutionName,
                last4,
                kind,
                "USD",
                null,
                null,
                reminder,
                hidden,
                closedOn,
                balance,
                balanceAsOf,
                snapshotCount);
    }
}
