package com.rankedfinance.accounts;

import static com.rankedfinance.accounts.AccountBuilder.account;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class AccountHealthTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 28);

    @Test
    void closedAccountsCountNowhere() {
        List<Account> accounts =
                List.of(account().closedOn(LocalDate.of(2026, 1, 1)).noSnapshot().build());
        assertThat(AccountHealth.summarize(accounts, TODAY)).isEqualTo(new AccountHealth(0, 0));
    }

    @Test
    void hiddenAccountsCountAsUpToDate() {
        List<Account> accounts = List.of(account().hidden().noSnapshot().build());
        assertThat(AccountHealth.summarize(accounts, TODAY)).isEqualTo(new AccountHealth(1, 0));
    }

    @Test
    void staleAccountsNeedAttention() {
        List<Account> accounts =
                List.of(
                        account().balanceAsOf(LocalDate.of(2026, 11, 20)).build(),
                        account().balanceAsOf(LocalDate.of(2026, 9, 1)).build());
        assertThat(AccountHealth.summarize(accounts, TODAY)).isEqualTo(new AccountHealth(1, 1));
    }
}
