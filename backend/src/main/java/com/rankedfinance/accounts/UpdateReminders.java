package com.rankedfinance.accounts;

import java.time.LocalDate;

/** Decides whether a manual account's balance is stale. Pure: pass the date in. */
public final class UpdateReminders {
    private UpdateReminders() {}

    public static boolean needsUpdate(Account account, LocalDate today) {
        if (account.closedOn() != null
                || account.hidden()
                || account.source() != AccountSource.MANUAL
                || account.updateReminder() == UpdateReminder.NEVER) {
            return false;
        }
        if (account.balanceAsOf() == null) {
            return true;
        }
        LocalDate asOf = account.balanceAsOf();
        LocalDate due =
                switch (account.updateReminder()) {
                    case TWO_WEEKS -> asOf.plusDays(14);
                    case MONTHLY -> asOf.plusMonths(1);
                    case QUARTERLY -> asOf.plusMonths(3);
                    case NEVER -> throw new IllegalStateException();
                };
        return today.isAfter(due);
    }
}
