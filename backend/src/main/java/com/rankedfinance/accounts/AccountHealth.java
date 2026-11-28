package com.rankedfinance.accounts;

import java.time.LocalDate;
import java.util.List;

/**
 * Counts of accounts that are fine and accounts that need an update. Closed accounts count in
 * neither.
 */
public record AccountHealth(int upToDate, int needsAttention) {
    public static AccountHealth summarize(List<Account> accounts, LocalDate today) {
        int ok = 0;
        int attention = 0;
        for (Account account : accounts) {
            if (account.closedOn() != null) {
                continue;
            }
            if (UpdateReminders.needsUpdate(account, today)) {
                attention++;
            } else {
                ok++;
            }
        }
        return new AccountHealth(ok, attention);
    }
}
