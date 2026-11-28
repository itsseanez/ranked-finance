package com.rankedfinance.accounts;

import static com.rankedfinance.accounts.AccountBuilder.account;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class DuplicateAccountMatcherTest {
    @Test
    void sameNameAndKindMatches() {
        Account existing = account().name("Checking").build();
        assertThat(
                        DuplicateAccountMatcher.matches(
                                account().name("Checking").build(), List.of(existing)))
                .containsExactly(existing);
    }

    @Test
    void nameIgnoresCaseAndSpaces() {
        Account existing = account().name("fidelity roth").build();
        assertThat(
                        DuplicateAccountMatcher.matches(
                                account().name(" Fidelity  Roth ").build(), List.of(existing)))
                .containsExactly(existing);
    }

    @Test
    void differentKindNeverMatches() {
        Account existing = account().kind(AccountKind.CASH).build();
        assertThat(
                        DuplicateAccountMatcher.matches(
                                account().kind(AccountKind.DEBT).build(), List.of(existing)))
                .isEmpty();
    }

    @Test
    void closedAccountsNeverMatch() {
        Account closed = account().closedOn(LocalDate.of(2026, 1, 1)).build();
        assertThat(DuplicateAccountMatcher.matches(account().build(), List.of(closed))).isEmpty();
    }

    @Test
    void institutionAndLast4Match() {
        Account existing =
                account()
                        .source(AccountSource.PROVIDER)
                        .name("Everyday")
                        .institution("Chase")
                        .last4("1234")
                        .build();
        Account candidate =
                account().name("Totally Different").institution(" chase ").last4("1234").build();
        assertThat(DuplicateAccountMatcher.matches(candidate, List.of(existing)))
                .containsExactly(existing);
    }

    @Test
    void sameLast4AtAnotherInstitutionDoesNotMatch() {
        Account existing = account().name("Everyday").institution("Chase").last4("1234").build();
        Account candidate = account().name("Savings").institution("Ally").last4("1234").build();
        assertThat(DuplicateAccountMatcher.matches(candidate, List.of(existing))).isEmpty();
    }
}
