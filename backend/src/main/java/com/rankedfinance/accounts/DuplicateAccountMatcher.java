package com.rankedfinance.accounts;

import java.util.List;
import java.util.Locale;

/** Finds existing accounts a new one would duplicate. Pure: no database. */
public final class DuplicateAccountMatcher {
    private DuplicateAccountMatcher() {}

    public static List<Account> matches(Account candidate, List<Account> existing) {
        return existing.stream().filter(other -> matches(candidate, other)).toList();
    }

    /** Strips, collapses inner whitespace, and lowercases. Null becomes the empty string. */
    public static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.strip().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private static boolean matches(Account a, Account b) {
        if (a.kind() != b.kind() || a.closedOn() != null || b.closedOn() != null) {
            return false;
        }
        if (hasInstitutionAndLast4(a) && hasInstitutionAndLast4(b)) {
            return normalize(a.institutionName()).equals(normalize(b.institutionName()))
                    && a.last4().equals(b.last4());
        }
        return a.source() == AccountSource.MANUAL
                && b.source() == AccountSource.MANUAL
                && normalize(a.name()).equals(normalize(b.name()));
    }

    private static boolean hasInstitutionAndLast4(Account account) {
        return !normalize(account.institutionName()).isEmpty()
                && account.last4() != null
                && !account.last4().isBlank();
    }
}
