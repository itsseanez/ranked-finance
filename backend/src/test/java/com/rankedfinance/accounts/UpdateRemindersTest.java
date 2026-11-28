package com.rankedfinance.accounts;

import static com.rankedfinance.accounts.AccountBuilder.account;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class UpdateRemindersTest {
    @Test
    void twoWeeksIsDueOnDay15() {
        Account a =
                account()
                        .reminder(UpdateReminder.TWO_WEEKS)
                        .balanceAsOf(LocalDate.of(2026, 11, 1))
                        .build();
        assertThat(UpdateReminders.needsUpdate(a, LocalDate.of(2026, 11, 15))).isFalse();
        assertThat(UpdateReminders.needsUpdate(a, LocalDate.of(2026, 11, 16))).isTrue();
    }

    @Test
    void monthlyIsDueTheDayAfterAMonth() {
        Account a =
                account()
                        .reminder(UpdateReminder.MONTHLY)
                        .balanceAsOf(LocalDate.of(2026, 10, 31))
                        .build();
        assertThat(UpdateReminders.needsUpdate(a, LocalDate.of(2026, 11, 30))).isFalse();
        assertThat(UpdateReminders.needsUpdate(a, LocalDate.of(2026, 12, 1))).isTrue();
    }

    @Test
    void monthlyFromJan31() {
        Account a =
                account()
                        .reminder(UpdateReminder.MONTHLY)
                        .balanceAsOf(LocalDate.of(2027, 1, 31))
                        .build();
        assertThat(UpdateReminders.needsUpdate(a, LocalDate.of(2027, 2, 28))).isFalse();
        assertThat(UpdateReminders.needsUpdate(a, LocalDate.of(2027, 3, 1))).isTrue();
    }

    @Test
    void quarterlyIsDueAfterThreeMonths() {
        Account a =
                account()
                        .reminder(UpdateReminder.QUARTERLY)
                        .balanceAsOf(LocalDate.of(2026, 8, 15))
                        .build();
        assertThat(UpdateReminders.needsUpdate(a, LocalDate.of(2026, 11, 15))).isFalse();
        assertThat(UpdateReminders.needsUpdate(a, LocalDate.of(2026, 11, 16))).isTrue();
    }

    @Test
    void hiddenNeverNeedsUpdate() {
        assertThat(
                        UpdateReminders.needsUpdate(
                                account().hidden().noSnapshot().build(), LocalDate.of(2030, 1, 1)))
                .isFalse();
    }

    @Test
    void closedNeverNeedsUpdate() {
        Account a = account().closedOn(LocalDate.of(2026, 1, 1)).noSnapshot().build();
        assertThat(UpdateReminders.needsUpdate(a, LocalDate.of(2030, 1, 1))).isFalse();
    }

    @Test
    void neverMeansNever() {
        Account a = account().reminder(UpdateReminder.NEVER).noSnapshot().build();
        assertThat(UpdateReminders.needsUpdate(a, LocalDate.of(2030, 1, 1))).isFalse();
    }

    @Test
    void providerAccountsAreNotRemindedHere() {
        Account a = account().source(AccountSource.PROVIDER).noSnapshot().build();
        assertThat(UpdateReminders.needsUpdate(a, LocalDate.of(2030, 1, 1))).isFalse();
    }

    @Test
    void noBalanceYetNeedsUpdate() {
        assertThat(
                        UpdateReminders.needsUpdate(
                                account().noSnapshot().build(), LocalDate.of(2026, 11, 28)))
                .isTrue();
    }
}
